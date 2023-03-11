package femto.semantics

import picolib._
import picolib.semantics._
import femto.ir._

// create a rule list from the entire file IR
def createAllRules(prog: Program): List[Rule] = prog match {
    case Program(Init(startState, startDir), stateDefs) =>
        // a map for linking state names to integers
        var stateMap = Map(startState + startDir.toString -> "0")
        var i = 1
        // add each of the other directions of the first state to the state map
        List(N, E, W, S) foreach { dir =>
            if dir != startDir then {
                stateMap ++= Map(startState + dir -> i.toString)
                i += 1
            }
        }
        // add all the other states to the map
        stateMap = fillStateMap(stateDefs, i, stateMap)

        var ruleList = List[Rule]()

        // build the rule list from the state definition list
        stateDefs foreach { stateDef =>
            ruleList ++= stateDefRules(stateDef, stateMap)
        }

        // remove unused rules (states that aren't used in all four directions)
        val (simpleRules, _) = simplifyRuleList(ruleList, State("0"),
                                                List[State]())
        simpleRules
}

// remove any states that are not used
def simplifyRuleList(rules: List[Rule], start: State, found: List[State]):
    Tuple2[List[Rule], List[State]] = {
    var simpleList = List[Rule]()
    var nextFound = found
    // avoid checking the same state more than once
    if !(found contains start) then {
        nextFound :+= start
        var nextStarts = List[State]()
        rules foreach {rule => rule match {
            case Rule(state, _, _, toState) =>
                // find rules that are part of state `start`
                if state == start then {
                    // add this state's rules to the simplified rule list
                    simpleList :+= rule
                    // add their `toState` to the list of states to check next
                    if toState != start then nextStarts :+= toState
                }
        }}
        // add the rules from each of the toStates
        nextStarts foreach { nextStart =>
            if !(nextFound contains nextStart) then {
                var (foundRules, newFound) = simplifyRuleList(rules, nextStart,
                                                              nextFound)
                simpleList ++= foundRules
                nextFound ++= newFound
            }
        }
    }
    (simpleList, nextFound)
}

// create a mapping of all the state names to integer names
def fillStateMap(stateList: List[StateDef], i: Int,
                 stateMap: Map[String, String]): Map[String, String] = {
    // base case
    if stateList.length <= 0 then stateMap
    else stateList.head match {
        case StateDef(name, _) =>
            // Don't add the same state multiple times
            if stateMap contains name + "N" then fillStateMap(stateList.tail,
                                                              i, stateMap)
            else {
                var mapAdd = Map[String, String]()
                var iter = 0
                // add mappings for each of the N, E, S, and W states
                List(N, E, W, S) foreach { dir =>
                    mapAdd ++= Map(name + dir -> (i + iter).toString)
                    iter += 1
                }
                // recursively add the rest of the mappings
                fillStateMap(stateList.tail, i + iter, stateMap ++ mapAdd)
            }
    }
}

// convert a state definition to a list of rules
def stateDefRules(state: StateDef, stateMap: Map[String, String]): List[Rule] =
    state match {
        case StateDef(name, commands) => {
            var rules = List[Rule]()
            // create rules for each of the directions
            List(N, E, W, S) foreach { dir =>
                // variable that holds the possible remaining states
                var allowed = List((Set[AbsoluteDir](), Set[AbsoluteDir]()))
                // add each command to the rule list and update remaining states
                commands foreach { command =>
                    val (ruleList, remain) = commandToRule(command, allowed,
                                                            name + dir.toString,
                                                            dir, stateMap)
                    rules ++= ruleList
                    allowed = remain
                }
            }
            rules
        }
}

// convert a command to a rule
def commandToRule(command: Command,
                  allowed: List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]],
                  state: String,
                  stateDir: AbsoluteDir,
                  stateMap: Map[String, String]):
                    Tuple2[List[Rule],
                           List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]] =
                            command match {
    case Command(Around(open, blocked), move, toState, nextDir) =>
        // convert the surrounding directions to absolute directions
        var absOpen = Set[AbsoluteDir]()
        var absBlocked = Set[AbsoluteDir]()
        open foreach { absOpen += toAbsDir(_, stateDir) }
        blocked foreach { absBlocked += toAbsDir(_, stateDir) }
        // add the `try` direction to the rule's surrounding check
        if move != Stay then absOpen = absOpen + toAbsDir(move, stateDir)

        var nextState = toState
        // use current state (minus the direction) if SELF is indicated
        if nextState == "SELF" then nextState = state take state.length - 1

        // determine what the surroundings should be for this command
        // in some cases, this con produce multiple surrounding states
        val (surrounds, remain) = makeSurround(absOpen, absBlocked, allowed)
        // create the rules
        var rules = List[Rule]()
        surrounds foreach { surr =>
            rules :+= Rule(
                State(stateMap(state)),
                surr,
                toMoveDir(move, stateDir),
                State(stateMap(nextState + toAbsDir(nextDir, stateDir).toString))
            )
        }

        (rules, remain)
}

// determine required surroundings from open and blocked and allowed checks
def makeSurround(open: Set[AbsoluteDir],
                 blocked: Set[AbsoluteDir],
                 allowed: List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]):
                    Tuple2[List[Surroundings],
                           List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]] = {
    var surrounds = List[Surroundings]()
    var remain = List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]()

    // ignore cases that have the same direction open AND blocked
    if (open intersect blocked).size == 0 then {
        allowed foreach { possible =>
            // add the surroundings if they work with what is possible
            if (possible(1) intersect open).size == 0 &&
               (possible(0) intersect blocked).size == 0 then {
                surrounds :+= toSurround(possible(0) ++ open,
                                         possible(1) ++ blocked)
            }
            // determine remaining possibilities for surroundings
            open foreach { dir =>
                if !(possible(0) contains dir) then {
                    remain :+= (possible(0), possible(1) + dir)
                }
            }
            blocked foreach { dir =>
                if !(possible(1) contains dir) then {
                    remain :+= (possible(0) + dir, possible(1))
                }
            }
        }
    }

    (surrounds, remain)
}

// convert a list of open and blocked absolute directions to a Surroundings
def toSurround(open: Set[AbsoluteDir],
               blocked: Set[AbsoluteDir]): Surroundings = {
    var surrs = List[RelativeDescription]()
    List(N, E, W, S) foreach {dir =>
        // prefer open if in both; would ideally throw an error/warn if in both
        if open contains dir then surrs :+= Open
        else if blocked contains dir then surrs :+= Blocked
        else surrs :+= Anything
    }
    Surroundings(surrs(0), surrs(1), surrs(2), surrs(3))
}

// convert a MoveDir (femto) to a MoveDirection (picolib)
def toMoveDir(dir: MoveDir, fromDir: AbsoluteDir): MoveDirection = dir match {
    case Stay => StayHere
    case other => toAbsDir(other, fromDir) match {
        case N => North
        case E => East
        case W => West
        case S => South
    }
}

// convert a relative direction to an absolute direction
def toAbsDir(dir: MoveDir, fromDir: AbsoluteDir): AbsoluteDir = dir match {
    case N => N
    case E => E
    case W => W
    case S => S
    case Forward => fromDir
    case Right => fromDir match {
        case N => E
        case E => S
        case W => N
        case S => W
    }
    case Reverse => fromDir match {
        case N => S
        case E => W
        case W => E
        case S => N
    }
    case Left => fromDir match {
        case N => W
        case E => N
        case W => S
        case S => E
    }
}