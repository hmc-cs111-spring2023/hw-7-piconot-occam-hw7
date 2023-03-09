# Building and running Femto

To run a program named `<program.bot>` on a maze `<maze.txt>`, you can use the
command `sbt run <maze.txt> <program.bot>`

The syntax for the bot file is as follows:

The file needs a `start` statement that indicates the starting state and
initial direction of the start state as an absolute direction (`^`, `>`, `<`,
`v`): `start startState >`.

The rest of the file should contain state definitions.
State definitions start with a header starting with `to`, and ending with `:`:
`to stateName:`.
The body of the state definition contains a series of rules.
Each definition must have at least one rule.

The rule syntax starts with an optional statement of surroundings in the form
`if <blocked> and not <open>` where `<blocked>` and `<open>` contain the
directions which can be absolute or relative (`left`, `right`, `forward`,
`reverse`).
They can be separated by commas, in parentheses, or not.

Examples: `if ^> and not left`, `if not (left, forward) and ^`, `if forward`.

After the surroundings statement, there can either be a `try` statement or a
`stay` statement.
The `try` statement takes a direction (`try left`) that indicates the direction
the bot will move _if the direction is open_.
A stay statement takes nothing and indicates the bot should not move.

After the movement statement, there needs to be a next state statement which
starts with `then` and is followed by the next state's name and the direction
of the next state: `then nextState forward`
Every next state needs a direction associated with it even if it doesn't use
relative directions (the direction is just ignored when the state doesn't use
relative directions).

So an entire rule would look something like `if v and not left try forward then nextState reverse`

Rules are separated by the `next` keyword.

The last rule can optionally be an `otherwise` statement that just takes a next state and state direction: `otherwise nextState <`.
This is used in the case that all other rules for this state were not used and indicates that the bot should `stay` and go to the next state.

Some shorthands are `try ahead` means `try forward then thisState forward` and `try nextState ^` means `try ^ then nextState ^`.

Extra notes about the syntax:

* All syntax is case insensitive
* State definition names can include any character other than whitespace and colons
* Rules always assume the preceding rules were not met, so if the last rule checked `if forward`, you don't need to say `if not forward` in the following statements!
* I didn't implement error handling, so if you enter illogical rules like `if not left and left`, it will fail silently by not including this rule.
Other illogical statements may fail with nondescriptive errors.