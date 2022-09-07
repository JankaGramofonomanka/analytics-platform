FROM python:3

COPY echo/src/main/python/requirements.txt  /app/requirements.txt
COPY echo/src/main/python/echo.py           /app/echo.py

WORKDIR /app

RUN pip install -r requirements.txt

EXPOSE 8080

CMD python3 echo.py
