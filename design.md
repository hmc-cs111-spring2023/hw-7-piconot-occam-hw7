# Design

## Who is the target for this design, e.g., are you assuming any knowledge on the part of the language users?

This design is for people familiar with maze-solving algorithms, but prefer to
use a combination of relative and absolute directions and also don't want to
explicitly think about what conditions have not been checked yet.

## Why did you choose this design, i.e., why did you think it would be a good idea for users to express the maze-searching computation using this syntax?

I think it simplifies the process when states are basically just repetitions of
each other but for the different directions (like in the right hand algorithm).

Additionally, automatically checking previous states makes it easier to just
state the new information that should be checked.

## What behaviors are easier to express in your design than in Picobot’s original design?  If there are no such behaviors, why not?

The biggest change is use of relative directions, so it should be easier to
indicate how to move in a state based on which direction you are going.
Another change is that if a command requires moving in a direction, it will
check if the bot can without the user needing to program it specifically.
The `otherwise` syntax allows users to have a catch-all way of defining motion
when none of the previous statements had conditions that were met.
Another big simplification is that following statements automatically assume
the previous statements were not met without needing to explicitly indicating
the negation.

## What behaviors are more difficult to express in your design than in Picobot’s original design? If there are no such behaviors, why not?

It should be impossible to:
* move in a direction that has a wall (`N*** -> N` or `**** -> N`)
* define a rule for a condition that is already handled (`N***` followed by
  `N*W*`)

It should be more difficult to see what exactly each statement is checking since
with this syntax, it depends on what statements are before it.

You can't indicate a next state without indicating a direction even if the next
state doesn't use any relative directions which could be a little extra.

Rules cannot be in any order within a state definition: the order determines how
it works!

## On a scale of 1–10 (where 10 is “very different”), how different is your syntax from PicoBot’s original design?

I'd say it's about an 8.
There are a lot of differences in how the directions are defined:
* `^><v` instead of `NEWS` and `not ^><v` instead of `xxxx`
* `forward reverse left right` for relative directions
* automatically removing conditions that have already been checked from
  following statements
* not needing to explicitly state wildcard directions

And the from states must be grouped in blocks with states declared using
user-defined names.
But each line in the state blocks _does_ indicate a condition (like `N**x`), a
movement (like `S`), and a state to go to (like `0`).

## Is there anything you would improve about your design?

I think a big thing would be to gracefully handle errors.
For example, if a user puts a statement that is already checked or doesn't make
sense, the program just handles it by not including it which could make
programs quite hard to debug.

Additionally, it could be interesting to allow for "absolute states" that only
use absolute directions and don't need a direction when they are the next state.