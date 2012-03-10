# Proseeo

Proseeo is a new way for groups to communicate, organize, and get stuff done. _"I am the cutest little bug tracking software there is!"_

What is Proseeo? Is it a bug tracking platform? Is it a process management tool? Is it a content management system? Is it a chat room or a wiki? *Proseeo is all of these.*

*pro-see-oh*

<span style="color:white; background-color:green; font-weight:bold">ok</span> **ok**

# Concepts

Proseeo is unlike any bug tracking software you have used. Before you get started there are few things you need to understand.

## Projects

A project is just a directory on your file system. This directory contains all your project data, including configuration, stories, and attachments. Proseeo makes finding and updating this data easy.

A project is just a directory, so to share a project, check it into source control. (A network file system or Dropbox would should work just as well.) There is no Proseeo server. Project configuration is stored in `project.proseeo`.

## Stories

A story is a like a ticket in a conventional bug database. For each story Proseeo keeps track of who created it, where it is, and what it contains. Stories contain "says" (tweet-like comments), attachments, and routing information.

A story is just a subdirectory under the project. Story metadata is stored in `script.proseeo`. Attachments are also stored in the story directory.

## Plans

Plans define what a story should contain. For example, the `bug` plan might contain fields for `title`, `description`, `fix_version`, and `test_notes`. A plan is a loose schema that defines the story requirements.

Proseeo believes that every story is different, and that no single workflow should be imposed. Each story has a plan file `plan.proseeo`. The plan file is in a simple text format and can be easily edited to suit the needs story.

Proseeo supports template plan files stored in the project directory. When a story is assigned a plan, the template plan is copied into the story directory. Proseo comes with template plan files `bug`, `todo`, and `release`.

## Routes

Routes determine where a story is, where it has been, and where it needs to go. Route destinations include individual users, and user groups. Because Proseeo does not impose a workflow the route is completely mutatable any any time. Proseeo trusts people to make the right routing decisions.

## Queries

later

<span style="color:white; background-color:green; font-weight:bold">ok</span> **ok**

# Getting Started

* Download and install Proseeo
* Create a project directory, `cd` into that directory, and execute `p init name`. Name is the name of your project.
* Create your first story using `p start name`. Name is the name of your story, and can be anything you want. You can use a generic name such as `bug` or something specific.
* later

<span style="color:white; background-color:green; font-weight:bold">ok</span> **ok**

# Development Notes

Here I am building a platform to help groups get stuff done. This is distributed
process structure. DPS. This is soft business process management.
This is a new way for groups to communicate, to organize, and to get stuff done.
I am starting with business of making software. This is the work I know, and the
first who will use this.

active chain and tip:
created by user A
A assigned to user B
B asked of user A
A responded
B assigned to group C
user D closed

pretty simple. just follow the arrows:
   created by establsihes the starting point
   assigned to user is a 1:1 arrow with no implied next destination
   ask of user is a 1:1 arrow with implied return to sender on "respond"
   assigned to group is a 1:* arrow with no implied next destination
   ask of group is a 1:* arrow with implied return to sender on "respond" by any 1 group member

we can now report on:
   stories that i am in the active chain
   stories that i am at the tip of the active chain
   stories that i am at the tip of the active chain and i am being asked something
   ditto for the last two, but due to group membership
these are reports against the story state itself, not the document state
they are built on built-in concepts in the story model
described above is the "routing" for stories

signing: a way to sign a story to assert authorship from th2is point back to the previous signing
encryption: a way to encrypt parts of the document?? i don't know. this is less interesting to me because i value building an open system. ideally with a slick front end that allows full text searching --- this data should be a repo for information.

git is clunky, that's ok, this is for the hardcore, a web interface can come later, a IOS interface too

could it work for the mongo database to actually live on the network and be shared? i think perhaps it is so!
  --- OR NOT! -- somebody's local repo could be out of date.

perhaps proseeo is business process management inspired by twitter

document templating

dsl to describe requirements?

/templates/bug <-- name of file

needs title:text
needs description:text
optional version_reported_in:named_enum(versions)

needs scrubbed:gate

needs version_assignment:named_enum(versions)

needs fixed_in_version:named_enum(versions)
needs release_note:text
needs fixed:gate

needs test_note:text
needs tested:gate

...
could the document itself somehow advise routing?
...

       title               : color on home page does not match(bright)
       description         : etc etc blah blah(bright)
  want version_reported_in : 2.4, 2.3, 2.2, 13 more(dim)

       scrubbed            : [/] by Landon Kuhn 2 day(s) ago(bright)

  need version_assignment  : 1.1(bright) 2.4, 2.3, 2.2, 13 more(dim)

       fixed_in_version    : 2.4(bright)
  need release_note        : > ____ < (dim)
  need fixed               : > [ ] < (dim)

       test_note(dim)
       tested
       foo_field
       bar_field

       14 more







what's next:

	tell needs to list attachments,
	plan needs to support attachments
  routing use cases: ask, pass
  indexer and queries
  generate template plans on init
  disable ansi when not supported or wanted
  brew formula
  bug: route list duplicates present
  .
  .
  .
  linking
  signing
  tagging
  eclipse task connector
  file encoding
  archiving
  jira import
  egit integration
  group member pinning
  command suggestions, undo

spinny:
  [-  ]
  [ - ]
  [  -]


script:
  attach "filename" by lkuhn @ ... # filename expected to be in story dir
  detach "filename" by lkuhn @ ...

p:
  p attach ~/Desktop/screenshot.jpg # file is copied into story dir as screenshot.jpg
  p attach ~/Desktop/foo/screenshot.jpg # this would fail
  p attach ~/Desktop/foo/screenshot.jpg as screenshot2.jpg # file is copied into story dir as screenshot2.jpg
  p detach screenshot.jpg # removes it from the script state, but not the file system
  p attach new release_document.md # touches release_document.md in story dir. perhaps in the future support a with template keywoard

release plan
  need version:text
  want summary:text
  need release_date:timestamp
  need release_document:attachment

  need accepted:gate

p set release_date now + 1d 1h # now and today keywords with + and - operators with human string operands
p set release_date now + 1d 2h
p set release_date today + 1d

p tell
  ...
1) screenshot.jpg
2) screenshot2.jpg

p open attachment 1 # invoke system viewer
p edit attachment 2 # invoke system editor

error messages should be written as if the programmer is sitting with the user and walking them through their use case
