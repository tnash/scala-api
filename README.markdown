## Example Scala project

###Requirements

Create an HTTP service for tracking progress of background jobs. You can use a programming language/frameworks of your choosing.

The job is represented by:

id - unique job’s identifier
total - a number representing total size of the job
progress - a number representing current progress of the job (ie. job is considered completed when progress == total)
The service interface should support the following actions:

Register a job - should accept total attribute, generate and return an ID for the job.
Update a job based on ID - should allow to increment job's progress by some arbitrary number or set absoluteprogress - returns the new progress. Expect to have a large volume (potentially concurrent) update requests to the same job.
Show all jobs
Show individual job by ID
Additional requirements:

Jobs should expire after they have not been updated for 1 minute (ie. they would no longer appear in Show all jobs, and would not be returned when showing that job by ID).
The job state should be stored in memory
Service input and output should be JSON
Include tests

###Solution:

An example Scala REST api app that leverages [Spray](spray.io), [Akka](akka.io), [ScalaCache](https://github.com/cb372/scalacache), and [Caffeine](https://github.com/ben-manes/caffeine). 

Spray and Akka were chosen because I needed a lightweight HTTP server in Scala. Spray is built on Akka. Scalacache provides the Scala wrapper around Caffeine. 

Caffeine is a high performant, concurrent caching library based on the Java ConcurrentMap and work done by Google in the Guava caching library. 

Caffeine manages expiring jobs that have not been updated for a period of time that exceeds the configured time to live. Scalacache provides the ability to swap out the implementation of the cache. Guava, Redis, Memcache, etc. can be used as the underlying caching library.

I chose not to recreate the wheel by implementing my own concurrent memory store and use a proven library, instead. I did have to provide an implementation of the asMap() method to Scalacache in order to expose the asMap() method of the Caffeine cache to get all of the jobs in the cache.

## Running
Clone to your computer and follow these steps to get started:

1. Launch SBT:

        $ sbt

4. Compile everything and run all tests:

        > test

5. Start the application:

        > run

6. Browse to [http://localhost:8080/jobs](http://localhost:8080/jobs)

### About
Configuration of the time to live (ttl) for the storage of jobs is defined in the application.conf file in the resources directory of the project.

The app exposes some basic endpoints.  
* A GET request to /jobs will return a list of all of the jobs stored in the system
* A PUT request to /jobs/[some job id] with the job progress defined in the body of the request will set the current progress of the job to the specified value.
* A PUT request to /jobs/[some job id] with the value to increment the current progress of the job will set the progress of the job to the current progress plus the increment value or the job total which ever is less.
* A POST request to /jobs with a Content-Type header of application/json will create a job from the json content in the body of the request.

Here are some curl commands you can try:

```
curl -v http://localhost:8080/jobs
curl -v http://localhost:8080/jobs/[a job id here]
curl -v -X POST http://localhost:8080/jobs -H "Content-Type: application/json" -d "{\"id\":\"abc\",\"total\": 100,\"progress\": 1}"
curl -v -X PUT http://localhost:8080/jobs/[id] -H "Content-Type: application/json" -d "{ \"progress\" : \"value\" }"
curl -v -X PUT http://localhost:8080/jobs/[id] -H "Content-Type: application/json" -d "{ \"increment\" : \"value\" }"
```

