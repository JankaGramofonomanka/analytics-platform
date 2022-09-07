FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1

COPY build.sbt  /app/build.sbt
COPY echo/      /app/echo
COPY project/   /app/project

WORKDIR /app/

EXPOSE 8080

RUN sbt "project echo" compile

CMD sbt "project echo" "~reStart"

