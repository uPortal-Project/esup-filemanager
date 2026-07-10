/**
 * Reads server-rendered application settings from data-* attributes.
 */
(function() {
    'use strict';

    const configEl = document.getElementById('esup-app-config');
    const data = configEl ? configEl.dataset : {};

    window.esupFileManagerConfig = {
        defaultPath: data.defaultPath || './',
        htmlFileTreeURL: data.htmlFileTreeUrl || './htmlFileTree',
        fileChildrenURL: data.fileChildrenUrl || './fileChildren',
        uploadFileURL: data.uploadFileUrl || './uploadFile',
        prepareCopyFilesURL: data.prepareCopyFilesUrl || './prepareCopyFiles',
        prepareCutFilesURL: data.prepareCutFilesUrl || './prepareCutFiles',
        pastFilesURL: data.pastFilesUrl || './pastFiles',
        detailsAreaURL: data.detailsAreaUrl || './detailsArea',
        createFileURL: data.createFileUrl || './createFile',
        renameFileURL: data.renameFileUrl || './renameFile',
        downloadFileURL: data.downloadFileUrl || './downloadFile',
        downloadZipURL: data.downloadZipUrl || './downloadZip',
        removeFilesURL: data.removeFilesUrl || './removeFiles',
        useDoubleClick: data.useDoubleClick === 'true'
    };

    const i18nMap = {
        uploadFileSuccess: 'i18nUploadFileSuccess',
        uploadFileFailed: 'i18nUploadFileFailed',
        uploadFilesSuccess: 'i18nUploadFilesSuccess',
        uploadFilesPartial: 'i18nUploadFilesPartial',
        uploadingProgress: 'i18nUploadingProgress',
        uploadProgress: 'i18nUploadProgress',
        uploadErrors: 'i18nUploadErrors',
        copyOk: 'i18nCopyOk',
        copyFailed: 'i18nCopyFailed',
        itemsCopied: 'i18nItemsCopied',
        cutOk: 'i18nCutOk',
        cutFailed: 'i18nCutFailed',
        itemsCut: 'i18nItemsCut',
        pasteOk: 'i18nPasteOk',
        pasteFailed: 'i18nPasteFailed',
        clipboardEmpty: 'i18nClipboardEmpty',
        removeOk: 'i18nRemoveOk',
        removeFailed: 'i18nRemoveFailed',
        deleteConfirmTitle: 'i18nDeleteConfirmTitle',
        deleteConfirmMessage: 'i18nDeleteConfirmMessage',
        deleteConfirmOk: 'i18nDeleteConfirmOk',
        deleteConfirmCancel: 'i18nDeleteConfirmCancel',
        deleteButton: 'i18nDeleteButton',
        renameTitle: 'i18nRenameTitle',
        renameButton: 'i18nRenameButton',
        renameSuccess: 'i18nRenameSuccess',
        renameFailed: 'i18nRenameFailed',
        createSuccess: 'i18nCreateSuccess',
        createFailed: 'i18nCreateFailed',
        createButton: 'i18nCreateButton',
        newFolderPlaceholder: 'i18nNewFolderPlaceholder',
        refresh: 'i18nRefresh',
        newFolder: 'i18nNewFolder',
        paste: 'i18nPaste',
        download: 'i18nDownload',
        copy: 'i18nCopy',
        cut: 'i18nCut',
        rename: 'i18nRename',
        delete_: 'i18nDelete',
        authOk: 'i18nAuthOk',
        authBad: 'i18nAuthBad',
        authMissingCredentials: 'i18nAuthMissingCredentials',
        authError: 'i18nAuthError',
        noFileSelected: 'i18nNoFileSelected',
        directoryLoadingError: 'i18nDirectoryLoadingError',
        refreshFailed: 'i18nRefreshFailed',
        sortFailed: 'i18nSortFailed',
        selectOneItem: 'i18nSelectOneItem',
        cannotFindFile: 'i18nCannotFindFile',
        cannotRetrieveFileName: 'i18nCannotRetrieveFileName',
        enterFolderName: 'i18nEnterFolderName',
        enterNewName: 'i18nEnterNewName',
        viewRefreshed: 'i18nViewRefreshed',
        nameUnchanged: 'i18nNameUnchanged',
        uploadConflictTitle: 'i18nUploadConflictTitle',
        uploadConflictMessage: 'i18nUploadConflictMessage',
        uploadConflictOverwrite: 'i18nUploadConflictOverwrite',
        uploadConflictCancel: 'i18nUploadConflictCancel'
    };

    window.i18n = Object.entries(i18nMap).reduce((messages, [key, dataKey]) => {
        messages[key] = data[dataKey] || '';
        return messages;
    }, {});

    document.addEventListener('click', (event) => {
        const button = event.target.closest('.js-language-switch');
        if (!button) return;

        const lang = button.dataset.lang;
        if (!lang) return;

        const url = new URL(window.location.href);
        url.searchParams.set('lang', lang);
        window.location.replace(url.toString());
    });
})();
