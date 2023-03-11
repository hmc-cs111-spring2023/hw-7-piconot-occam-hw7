# Evaluation: running commentary

## External DSL

_Describe each change from your ideal syntax to the syntax you implemented, and
describe_ why _you made the change._

**On a scale of 1–10 (where 10 is "a lot"), how much did you have to change your syntax?**

I changed very little of my syntax (so ~ 1.5) and most of my changes were just
realizing I didn't like what I had originally put.
The trade off, however, was that it took a lot of time to get all the changes I
wanted to work.

One thing I considered adding, but eventually decided against was being able to
define a `nextState` without 

**On a scale of 1–10 (where 10 is "very difficult"), how difficult was it to map your syntax to the provided API?**

It was pretty difficult -- I'd say maybe an 8 -- since the nature of how the
syntax worked was pretty different from how picobot runs.
The main thing I struggled with was figuring out how to make sequential commands
that automatically made sure the interpreted commands excluded possibilities
from previous commands.
The relative directions were a little tricky, but mostly I just went with a sort
of verbose solution that got the job done.