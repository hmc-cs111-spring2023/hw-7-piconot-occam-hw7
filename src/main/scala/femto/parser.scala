package femto.parser

import scala.util.parsing.combinator._
import femto.ir._
import scala.collection.Factory

object FemtoParser extends JavaTokenParsers {

    def apply(s: String): ParseResult[Program] = parseAll(prog, s.toLowerCase)

    // parsing an entire file
    def prog: Parser[Program] =
        // if start statement is at the start
        start ~ rep1(stateDef) ^^ {
            case init ~ defs => Program(init, defs)
        }
        // if it's not at the start
        | rep1(stateDef) ~ start ~ rep(stateDef) ^^ {
            case defs ~ init ~ moreDefs => Program(init, defs ++ moreDefs)
        }

    // parsing the initial state (where to start)
    def start: Parser[Init] =
        "start" ~> stateName ~ absDir ^^ { case name ~ d => Init(name, d) }

    // parsing a state definition (list of rules for a state)
    def stateDef: Parser[StateDef] =
        "to" ~> stateName ~ ":" ~ repsep(command, "next") ~ opt(otherwise) ^^ {
            case name ~ ":" ~ commands ~ other =>
                StateDef(name, commands ++ other)
        }

    // parsing a command (like a line of picobot code)
    def command: Parser[Command] =
        // "try ahead" syntax
        surround ~ "try ahead" ^^ {
            case surr ~ "try ahead" =>
                Command(surr, Forward, "SELF", Forward)
        }
        // "try nextState direction" syntax
        | surround ~ "try" ~ stateName ~ dir ^^ {
            case surr ~ "try" ~ toState ~ stateDir =>
                Command(surr, stateDir, toState, stateDir)
        }
        // "try move then nextState direction" syntax
        | surround ~ "try" ~ dir ~ "then" ~ stateName ~ dir ^^ {
            case surr ~ "try" ~ move ~ "then" ~ toState ~ stateDir =>
                Command(surr, move, toState, stateDir)
        }
        // "stay" syntax
        | surround ~ "stay then" ~ stateName ~ dir ^^ {
            case surr ~ "stay then" ~ toState ~ stateDir =>
                Command(surr, Stay, toState, stateDir)
        }

    // parsing an "otherwise" command
    def otherwise: Parser[Command] =
        "otherwise" ~> stateName ~ dir ^^ {
            case toState ~ stateDir =>
                Command(Around(List(), List()), Stay, toState, stateDir)
        }

    // parsing the part of a command that states the bot's surroundings
    def surround: Parser[Around] =
        // blocked stated before open
        "if" ~> dirList ~ "and not" ~ dirList ^^ {
            case blocked ~ "and not" ~ open =>
                Around(open, blocked)
        }
        // open stated before blocked
        | "if not" ~> dirList ~ "and " ~ dirList ^^ {
            case open ~ "and" ~ blocked =>
                Around(open, blocked)
        }
        // only blocked stated
        | "if" ~> dirList ^^ { case blocked => Around(List(), blocked) }
        // only open stated
        | "if not" ~> dirList ^^ { case open => Around(open, List()) }
        // surroundings not stated
        | "" ^^^ Around(List(), List())

    // parsing a list of directions (for indicating surroundings)
    def dirList: Parser[List[Dir]] =
        opt("(") ~> rep1sep(dir, opt(",")) <~ opt(")")

    // parsing a direction (absolute or relative)
    def dir: Parser[Dir] =
        "forward" ^^^ Forward
        | "right" ^^^ Right
        | "left" ^^^ Left
        | "reverse" ^^^ Reverse
        | absDir

    // parsing an absolute direction
    def absDir: Parser[AbsoluteDir] =
        "^" ^^^ N
        | ">" ^^^ E
        | "<" ^^^ W
        | "v" ^^^ S

    // regex for legal state names (any non-whitespace or non-colon)
    def stateName = "[^\\s\\:]+".r
}