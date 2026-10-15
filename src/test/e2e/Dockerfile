FROM mcr.microsoft.com/playwright:v1.56.1-noble@sha256:f1e7e01021efd65dd1a2c56064be399f3e4de00fd021ac561325f2bfbb2b837a
USER root
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-venv ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /e2e
COPY support/requirements.txt /e2e/support/requirements.txt
RUN python3 -m venv /venv && /venv/bin/pip install --no-cache-dir -r support/requirements.txt
ENV PATH="/venv/bin:${PATH}"
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run check

ENTRYPOINT ["bash", "scripts/test.sh"]
