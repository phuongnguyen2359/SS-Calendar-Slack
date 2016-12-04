# Project - *SS Calendar Slack*

## User Stories

Background & Overview: Silicon Straits office has 5 meeting rooms. We are currently using paper-based method to let our staff book the rooms. At each meeting room’s door, we place a printed calendar, when someone wants to reserve a meeting; he/she has to shade the time slots on the paper to indicate the booking.

Silicon Straits are hoping to replace these paper calendars with a modern LCD device that shows the room’s availability. Moreover, Silicon Straits are using Slack extensively for team communication; hence they would also require the team to implement a deep integration with Slack such that one can manage his bookings entirely via Slack. Silicon Straits do not have a fixed budget for this project; the team is free to propose a suitable approach to our needs with cost for our considerations. Once the technical approach has been approved by us, Silicon Straits will provide the team with necessary devices and hardware components to work on.

The following **required** functionality is completed:

- [x] Users can chat with ChatBot to book a room. Example: users simply type Book + name the room.
- [x] User can receive notification 5 minutes before booking.
- [x] User can cancel their own bookins.
- [x] Slack integration via RESTful APIs.
- [x] Slack-based authentication, no external authentication is allowed.

The following **required** Device:
- [x] 6 inches LCD or above.
- [x] Touch is nice to have.
- [x] If LCD is touchable, we expect your team to implement full touch interaction at the device.



A barebones Java app, which can easily be deployed to Heroku.

This application supports the [Getting Started with Java on Heroku](https://devcenter.heroku.com/articles/getting-started-with-java) article - check it out.

[![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

## Running Locally

Make sure you have Java and Maven installed.  Also, install the [Heroku Toolbelt](https://toolbelt.heroku.com/).

```sh
$ git clone https://github.com/heroku/java-getting-started.git
$ cd java-getting-started
$ mvn install
$ heroku local:start
```

Your app should now be running on [localhost:5000](http://localhost:5000/).

If you're going to use a database, ensure you have a local `.env` file that reads something like this:

```
DATABASE_URL=postgres://localhost:5432/java_database_name
```

## Deploying to Heroku

```sh
$ heroku create
$ git push heroku master
$ heroku open
```

## Documentation

For more information about using Java on Heroku, see these Dev Center articles:

- [Java on Heroku](https://devcenter.heroku.com/categories/java)
