package femto.ir

trait MoveDir
case object Stay extends MoveDir

trait Dir extends MoveDir
case object Forward extends Dir
case object Right extends Dir
case object Left extends Dir
case object Reverse extends Dir
case object Empty extends Dir

trait AbsoluteDir extends Dir
case object N extends AbsoluteDir
case object E extends AbsoluteDir
case object W extends AbsoluteDir
case object S extends AbsoluteDir

case class Around(open: List[Dir], blocked: List[Dir])

case class Command(around: Around, move: MoveDir, toState: String, stateDir: Dir)

case class StateDef(name: String, commands: List[Command])

case class Init(toState: String, stateDir: AbsoluteDir)

case class Program(start: Init, stateCommands: List[StateDef])