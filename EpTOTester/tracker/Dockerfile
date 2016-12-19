FROM python:3.5.2-alpine

RUN pip install pydevd

COPY tracker.py /code/
WORKDIR /code

EXPOSE 4321

CMD [ "python", "./tracker.py" ]
