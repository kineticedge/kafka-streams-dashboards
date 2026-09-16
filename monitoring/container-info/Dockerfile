FROM python:3.11-slim

RUN pip install requests requests-unixsocket

COPY exporter.py /exporter.py

CMD ["python", "/exporter.py"]

