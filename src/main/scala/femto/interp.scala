package femto.semantics

import picolib._
import picolib.semantics._
import femto.ir._

def createAllRules(prog: Program): List[Rule] = prog match {
    case Program(Init(startState, startDir), stateDefs) =>
        var stateMap = Map(startState + startDir.toString -> "0")
        var i = 1
        List(N, E, W, S) foreach { dir =>
            if dir != startDir then {
                stateMap ++= Map(startState + dir -> i.toString)
                i += 1
            }
        }
        stateMap = fillStateMap(stateDefs, i, stateMap)

        var ruleList = List[Rule]()

        stateDefs foreach {stateDef => ruleList ++= stateDefRules(stateDef, stateMap) }

        val (simpleRules, _) = simplifyRuleList(ruleList, State("0"), List[State]())
        simpleRules
}

def simplifyRuleList(rules: List[Rule], start: State, found: List[State]):
    Tuple2[List[Rule], List[State]] = {
    var simpleList = List[Rule]()
    var nextFound = found
    if !(found contains start) then {
        nextFound :+= start
        var nextStarts = List[State]()
        rules foreach {rule => rule match {
            case Rule(state, _, _, toState) =>
                if state == start then {
                    simpleList :+= rule
                    if toState != start then nextStarts :+= toState
                }
        }}
        nextStarts foreach { nextStart =>
            if !(nextFound contains nextStart) then {
                var (foundRules, newFound) = simplifyRuleList(rules, nextStart, nextFound)
                simpleList ++= foundRules
                nextFound ++= newFound
            }
        }
    }
    (simpleList, nextFound)
}

def fillStateMap(stateList: List[StateDef], i: Int,
                 stateMap: Map[String, String]): Map[String, String] = {
    if stateList.length <= 0 then stateMap
    else stateList.head match {
        case StateDef(name, _) =>
            if stateMap contains name + "N" then fillStateMap(stateList.tail, i, stateMap)
            else {
                var mapAdd = Map[String, String]()
                var iter = 0
                List(N, E, W, S) foreach { dir =>
                    mapAdd ++= Map(name + dir -> (i + iter).toString)
                    iter += 1
                }
                fillStateMap(stateList.tail, i + iter, stateMap ++ mapAdd)
            }
    }
}

def stateDefRules(state: StateDef, stateMap: Map[String, String]): List[Rule] = state match {
    case StateDef(name, commands) => {
        var rules = List[Rule]()
        List(N, E, W, S) foreach { dir =>
            var allowed = List((Set[AbsoluteDir](), Set[AbsoluteDir]()))
            commands foreach { command =>
                val (ruleList, remain) = commandToRule(command, allowed, name + dir.toString, dir, stateMap)
                rules ++= ruleList
                allowed = remain
            }
        }
        rules
    }
}

def commandToRule(command: Command,
                  allowed: List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]],
                  state: String,
                  stateDir: AbsoluteDir,
                  stateMap: Map[String, String]):
                    Tuple2[List[Rule], List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]] = command match {
    case Command(Around(open, blocked), move, toState, nextDir) =>
        var absOpen = Set[AbsoluteDir]()
        var absBlocked = Set[AbsoluteDir]()
        open foreach { absOpen += toAbsDir(_, stateDir) }
        blocked foreach { absBlocked += toAbsDir(_, stateDir) }
        if move != Stay then absOpen = absOpen + toAbsDir(move, stateDir)

        var nextState = toState
        if nextState == "SELF" then nextState = state take state.length - 1

        val (surrounds, remain) = makeSurround(absOpen, absBlocked, allowed)
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

def makeSurround(open: Set[AbsoluteDir],
                 blocked: Set[AbsoluteDir],
                 allowed: List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]):
                    Tuple2[List[Surroundings], List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]] = {
    var surrounds = List[Surroundings]()
    var remain = List[Tuple2[Set[AbsoluteDir], Set[AbsoluteDir]]]()

    if (open intersect blocked).size == 0 then {
        allowed foreach { possible =>
            if (possible(1) intersect open).size == 0 &&
               (possible(0) intersect blocked).size == 0 then {
                surrounds :+= toSurround(possible(0) ++ open, possible(1) ++ blocked)
            }
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

def toSurround(open: Set[AbsoluteDir],
               blocked: Set[AbsoluteDir]): Surroundings = {
    var surrs = List[RelativeDescription]()
    List(N, E, W, S) foreach {dir =>
        if open contains dir then surrs :+= Open
        else if blocked contains dir then surrs :+= Blocked
        else surrs :+= Anything
    }
    Surroundings(surrs(0), surrs(1), surrs(2), surrs(3))
}

def toMoveDir(dir: MoveDir, fromDir: AbsoluteDir): MoveDirection = dir match {
    case Stay => StayHere
    case other => toAbsDir(other, fromDir) match {
        case N => North
        case E => East
        case W => West
        case S => South
    }
}

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