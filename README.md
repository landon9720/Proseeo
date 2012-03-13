# Proseeo

Proseeo is a new way for groups to communicate, organize, and get stuff done. _"I am the cutest little bug tracking software there is!"_

What is Proseeo? Is it a bug tracking platform? Is it a process management tool? Is it a content management system? Is it a chat room or a wiki? *Proseeo is all of these.*

*pro-see-oh*

**ok** ok

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

Plans are just suggestions about what your story should contain. Each field is loosely validated based on "needs" and "wants" and a simple type system. Ultimately it is up to you to decide what each story should contain, based on what makes sense. Proseeo happily stays out of your way.

Proseeo supports template plan files stored in the project directory. When a story is assigned a plan, the template plan is copied into the story directory. Proseo comes with template plan files `bug`, `todo`, and `release`.

## Routes

Routes determine where a story is, where it has been, and where it needs to go. Route destinations include individual users, and user groups. Because Proseeo does not impose a workflow the route is completely mutatable any any time. Proseeo trusts people to make the right routing decisions.

## Queries

later

**ok** ok

# Getting Started

* Download and install Proseeo
* Create a project directory, `cd` into that directory, and execute `p init name`. Name is the name of your project.
* Create your first story using `p start name`. Name is the name of your story, and can be anything you want. You can use a generic name such as `bug` or something specific.
* later

**ok** ok

# Development Notes

## what's next for v0.01

* routing use cases: ask, pass
* indexer and queries
* disable ansi when not supported or wanted
* brew formula
* top of plan: starting route, including support for {creator}
* prevent p start . from "working"

## what's next for future release

* linking
* signing & encryption?
* tagging
* eclipse task connector
* file encoding
* archiving
* jira import
* egit integration
* group member pinning
* command suggestions, undo