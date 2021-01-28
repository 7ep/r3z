package coverosR3z.uitests

import coverosR3z.bddframework.UserStory

object TimeEntryUserStory : UserStory(
    key = "TimeEntry",
    story =
    """
   As an employee
   I want to have a highly-efficient means to enter time 
   So that I can work with my time entries without really having to think about the tool
    """
) {
    init {

        addScenario(
            "timeentry - An employee should be able to enter time for a specified date",

                "Given the employee worked 8 hours yesterday,",
                "when the employee enters their time,",
                "then time is saved."
        )

        addScenario(
            "timeentry - should be able to submit time for a certain period",

                "Given that I am done entering my time for the period",
                "When I submit my time",
                "Then the time period is ready to be approved"
        )

        addScenario(
            "timeentry - should be able to unsubmit a period",

                "Given that I had submitted my time but need to make a change",
                "When I unsubmit my time",
                "Then the time period is ready for more editing"
        )

        addScenario(
            "timeentry - should be possible to disallow time entry on future days for certain projects",

                "Given I am working on a project for the government that disallows forward entry",
                "when I try to enter time tomorrow",
                "then the system disallows it."
            )

        addScenario(
            "timeentry - I should see my existing time entries when I open the time entry page",

                "Given I had previous entries this period",
                "when I open the time entry page",
                "then I see my prior entries"
        )

    }
}



/*
Scenario: should be able to enter time
Scenario: should be able to submit time for a certain period
Scenario: Should be able to enter time on multiple projects and days throughout time period
Scenario: a blind person should have equivalent accommodation for entering time
Scenario: should be possible to disallow time entry on future days for certain projects
Scenario: I should see my existing time entries when I open the time entry page
Scenario: When I enter a time entry, I want to enter a comment explaining the entry
Scenario: When I enter a time entry, I want to enter a time spent
Scenario: When I enter a time entry, I want to enter a project

Explore: what is the metaphor we want for the UI/UX? Calendar? Tabbing? (non-mouse nav)?

Test: all data entry should be automatically saved as I work
Test: all inputs should restrict to valid values
Test: Can enter time easily on Mac, Windows common browsers
Test: Can enter time on phone browsers - Android and IPhone
Test: should be an option to quickly enter time per project or per day (that is, can I tab
    through the days or through the projects on a given day)
Test: a new time period should typically populate default projects

goal: time entry on a given page should be ordered in a way to ease input

General notes:
--------------

Matt:
when I edit a time entry and hit "save", I should remain on the view entries screen
    (with the save button returning to 'edit' and the input boxes re-locking)
when I edit a time entry and try to modify the date field, a calendar form will open up
when I edit a time entry, I can modify the project field from a dropdown + typable set of projects
From the 'view entries' page, I can delete a time entry, verify the action, and see it removed
I can create a new time entry in the 'view entries' page
when I click the 'edit' button on an existing entry, the fields should become editable
    without immediately resizing
when my time entry details are longer than ___, they are truncated  unless I hover/select/something
    the details box on that entry
when I go to a page in the timekeeping application, default CSS should be applied, and forms
    should be centered in the screen
A side note that is hard to phrase in a story: I don't want to make it easy to make multiple
    days of entries at the same time through any form of batch entry, because that is an inappropriate way of entering time that we should discourage. Plus more complexity
click when start, click when done, on a project.
useful defaults.  New entry - 8 hours. Maybe.
time on the UI should be hours and portions of.
I enter time at end of day typically, sometimes next day
the more complicated my day is, less I like to enter time.  Will generally assume day is one
    thing, will break that up to smaller bits as reality intrudes.

Rich:
When I enter time, I want to see my existing time entries to know what remaining entries I need to enter
When I enter time, I want to enter a comment explaining the entry
When I enter time for a day, I want to be able to record multiple projects that I worked on that day
When I enter time, I want a visual presentation similar to a calendar that I can easily tab
    through without using the mouse a lot
There are ‘inappropriate’ vs. ‘unsupported’ (or even ‘difficult’) arguments that get to
    realism. Whether right or wrong, I frequently enter multiple days worth of time.
    While this may be inappropriate for government auditability, I’m not sure that is a
    current requirements of the system.
In my particular use case, I write my time down elsewhere (specifically: I draw entries in
    my calendar each day) and periodically enter the time into whatever pain-in-the-ass time
    system I’m required to use.
Oooh. Interesting interface idea: allow time-entry in “calendar” style appointments. Draw
    your entries on your calendar and decorate them in a way that links time entry into a time system.
In my example picture: half the appointments are pre-scheduled and are just meetings that
    I go to. Some of them are post-drawn entries where I plug in what I did after-the
    fact (e.g. “lab research” where I went off and read about kotlin and checked out some code).
What I’d REALLY love to do is click on a particular time entry and asociate it with a cost
    center/project such that it adds up my time for me. Disclaimer: I’d probably HATE
    this after using for a while.
"the time system that I don't hate" TTSTIDH
time and expenses intrinsically different
time and expenses both equate to money
batch process entry: week of time at a time.
most common projects at top
Present to user what they like to see - week at a time
I draw my time spent on a calendar, throughout the day
There's a bit of reverse engineering to figure out what was done in a day, on occasion.
scribble: d:1.5, v:6.5
an entry possibility: start with default: v:9.5.  Adding in new items subtracts from
    total. Adding in d:1.5 would pull vibrent down to 8.
choose your mode

Jenna:
a button on desktop, dropdown: project, click button to start and stop time on project
I write notes about time on paper, fill in later

Mitch:
auto-populate with projects over last two time periods
wait until second notification "you are bad!!!" and then do it, each day

Extracted essentials:
---------------------
Easy to select projects that I'm entering time in
Easy to add time on a day
Easy to track your work on a project
Easy to focus on an (adjustable) time period, to see it all at once
minimal clicks
highly efficient for most-commonplace
meaning conveyed through colors
autosaving as you work, maybe with subtle indicators as it saves
connections with outside systems like google calendar
should be very fast and easy to enter time from multiple times / days
There is some sense of confirmation of action, and some variation - depending on impact, more
    levels of checking before and announcing after.
tabbing order must be considered - highest priority depends on context (maybe a setting for
    whether tabbing goes vertical or horizontal)
may be disallowed to enter time in future days, depending on project
may be necessary to have time entered for previous day by certain time OR ELSE
time-entry page will have default projects intelligently pre-entered
mode of entry: each item like normal, or add in first project with total hours of day and each
    new entry subtracts from the total
levels of selecting projects:
    level 1 - last week's projects.
    level 2 - common projects over last couple months.
    level 3 - ways to search for any project available to this user

Byron random thoughts:
fill in each day
graphical?
validation?
accessibility?
transferable to mobile?
Simple
how does tracking during work work?
keep view down to worked projects (no need to see everything every time period)
L1, L2, L3 cache - faster

Product owner's prioritized scenarios:
Entering time for a project, minimal clicks
Adding a new line item
A new charge code?
Entering time in periods
Submitting a period
default convention: database changes as user provides input, calling to API, perhaps with
    slight delay, with simple expression that the system is not synced / synced
validating all entries
entering time with previous period submission - default projects set




 */
