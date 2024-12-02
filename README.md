# nem

This project uses Quarkus <https://quarkus.io/>. You will need JDK 21 and Docker to run it.

You can run the application in dev mode using:

```shell script
./gradlew quarkusDev
```

Quarkus ships with a Dev UI, which is available at <http://localhost:8080/q/dev/>. Endpoint documentation is available at <http://localhost:8080/q/swagger-ui/>

Build the application (and run the tests) using

```shell script
./gradlew build
```

You can run the formatter using `./gradlew spotlessApply`

Incidentally, if you are using IntelliJ IDEA you can just open the project since it fully supports Quarkus applications. You can then use the `examples.http` file to try some data.

Otherwise, you can upload some data using curl:

```sheel script
curl http://localhost:8080/meter_reading -XPOST -H'content-type: text/csv' --data-binary '@src/test/resources/large_file.csv'
```

## Q1. What does your repository do?

This repository contains a Quarkus application that has a single endpoint. Posting a NEM12 file to the endpoint will
store the data in a PostgreSQL database.

## Q2. What are the advantages of the technologies you used for the project?

The main advantage of using Quarkus for this assigment is that I'm familiar with it. In general, I would say a big
advantage of Quarkus over e.g. Spring Boot is that it supports 'dev containers' out of the box: it will use
Docker to start a database / message broker / kafka instance for you during development without any configuration.

By default Quarkus uses Java, but I switched to Kotlin because I understand that's what Flo uses. I would say the big
advantage of Kotlin over modern Java is it's null handling, and allowing scope functions like `.let {}` to write code
that reads from top to bottom. Of course, other people might see other advantages such as Kotlin Web / Kotlin Native and
coroutines, but I think those are of less importance since other ecosystems are getting better as well
(e.g. typescript / rust / virtual threads in Java).

### Q3. How is the code designed and structured?

The code is structured using the standard Java / Kotlin conventions and "Vertical Slice Architecture". Since the project
is small it's not really noticeable though. Traditionally, Java developers don't like putting DB calls directly into API
endpoints, but I'm not a fan of overly abstracting code at the beginning, so I think it's fine like this for the initial
phase. As the project grows some time should be spent on abstracting access to the data (and hopefully the company has
some preferred way of doing so)

## Q4. How does the design help to make the codebase readable and maintainable for other engineers?

Following project conventions is good because:

1. you don't need to fight the framework
2. developers can just read the official documentation online, instead of having to maintain internal documentation

The project comes with automatic code formatters which should avoid any discussions about tabs vs spaces. It also helps
with PR readability, since most formatters are designed to produce minimal line changes.

Tests are set up to use the public endpoint(s). This promotes testing for actual business requirements and avoids
coupling tests to internal code details. (If you only test business requirements using public endpoints then tests should
never break even when doing large refactorings)

I didn't set up any linting on this project, but I would assume there would be some on a real project.

## Q5. Discuss any design patterns, coding conventions, or documentation practices you implemented to enhance readability and maintainability.

I'm a big fan of Domain Driven Design and hexagonal-like architecture, but this project is too small to really show this.

There is a generated OpenAPI spec for the API, which should help other engineers when they need to integrate with the service.

## Q6. What would you do better next time?

There is definitely room for improvement in the comments. Especially linking back to the specification could be improved.

The parser exceptions are not great. This is also seen in the tests where I had to resort to asserting on the exception message.
I would probably create specific exceptions if I had more time.

Error response are not great either. I like using RFC 9457 for error handling (there is a Quarkus plugin for this)
because the 'application/problem' content-type makes it very easy to handle on the client.

The code contains a bunch of TODOs because I either didn't think they were important for this assigment, or because
there was some ambiguity. Most of the things that where unclear should have been resolved before even starting to code
(things like "where does the data come from?", "do we reject duplicate data or ignore it?", "what are we going to do
with this data in the first place?", and "security?"). For the sake of this assigment, I just chose to implement
something instead of asking for clarification.

## Q7. Reflect on areas where you see room for improvement and describe how you would approach them differently in future projects.

One large issue with using a REST endpoint for this is that AWS load balancers have a fixed timeout of 60 seconds,
making API calls inconvenient for long-running operations. In reality, I would probably not be writing a REST endpoint
for this at all. I'm assuming the actual data would come from an SFTP server or through some kind of queue.
(I have no idea what B2B e-Hub is)

I chose Quarkus because of the limited time. I would not choose to use Quarkus + Kotlin for any serious project at this
point in time and would either use Quarkus + Java or some other framework.

I would definitely choose some kind of library for executing SQL, over plain JDBC.

A quick test trying to insert 245000 interval data (200) records takes 2m41s on my machine, so that's 132 million
records per day. Assuming an energy meter creates 1 record per day, this performance would probably be acceptable for
now? While inserting data there is 100% disk usage, so getting a faster disk, multiple disk in raid, or partitioning the
table space seems like good ways to quickly improve performance further. There might also be other ways to tune the DB
that I'm not aware of, like tweaking checkpoint creation. Give the (small) size of the data, I would assume there is more
performance to be had somewhere on the DB side.

## Q8. What other ways could you have done this project?

The possibilities here are really endless. This assigment does not have a business requirement, only a technical one.
If I could do something differently, then it would be to get a business requirement so it's clear why I'm implementing
this.

## Q9. Explore alternative approaches or technologies that you considered during the development of the project.

Since this project is so small, I considered writing it as a simple script in Typescript using Deno. Eventually, I
decided I wanted to use Kotlin since I assume that's what used on the backend side in your company (and this looks
more like a backend issue).

Since Quarkus + Kotlin does still have some issues, I also briefly considered using a Kotlin native framework like
Ktor. But given the limited time I decided to use something I'm familiar with.

One of the pain points of using Kotlin with Quarkus is the ORM (Hibernate). I already knew I wanted to use a COPY
statement, and most ORMs don't support this, I decided to skip any form of ORM for this project completely.

I didn't want to touch the postgres requirement, but reading the spec I did wonder if it might be possible to just read
the data directly from the NEM12 files when needed. I'm just brainstorming here, but it might be a good idea to keep
the original data available somewhere anyway (as a backup?) + the format is not very complex + would fit into memory =
should be pretty fast to just read from the filesystem and store in memory
