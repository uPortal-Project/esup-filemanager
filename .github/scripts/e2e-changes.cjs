const { spawnSync } = require('node:child_process');

module.exports = async ({ github, context, core }) => {
    // Explicit requests always run, including a repeat on the same commit.
    if (context.eventName !== 'schedule') {
        core.setOutput('run', 'true');
        return;
    }

    let baseline;
    try {
        const { data } = await github.rest.actions.listWorkflowRuns({
            ...context.repo,
            workflow_id: 'e2e.yml',
            branch: context.payload.repository.default_branch,
            status: 'success',
            per_page: 100,
        });
        for (const run of data.workflow_runs) {
            // A skipped weekly job must never become the new baseline.
            const jobs = await github.paginate(github.rest.actions.listJobsForWorkflowRun, {
                ...context.repo, run_id: run.id, per_page: 100,
            });
            if (jobs.some(job => job.name === 'E2E full (direct)' && job.conclusion === 'success')) {
                baseline = run.head_sha;
                break;
            }
        }
    } catch (error) {
        core.warning(`Cannot read E2E history; running the suite: ${error.message}`);
    }

    let shouldRun = true;
    if (baseline) {
        const diff = spawnSync('git', [
            'diff', '--quiet', baseline, 'HEAD', '--',
            'pom.xml', 'src/main', 'src/test', 'src/etc', '.github',
            ':(glob,exclude)**/*.md',
        ], { encoding: 'utf8' });
        // Missing history (for example after a force push) also runs the suite.
        shouldRun = diff.status !== 0;
        if (diff.error || diff.status > 1 || diff.status === null) {
            core.warning(`Cannot compare ${baseline}; running the suite.`);
        }
    }
    core.info(baseline ? `Last successful full/direct run: ${baseline}` : 'No successful full/direct baseline found.');
    core.setOutput('run', String(shouldRun));
    await core.summary.addRaw(shouldRun
        ? 'Full E2E campaign required.'
        : 'E2E skipped: no relevant changes since the last successful full/direct campaign.').write();
};
