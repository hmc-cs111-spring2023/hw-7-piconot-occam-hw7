// This file was modified from the example code

import Console.{RED, RESET}

import picolib.maze._
import picolib.semantics._
import picolib.display._
import femto.ir._
import femto.semantics._
import femto.parser.FemtoParser

@main
def main(args: String*) = {

    // Error handling: did the user pass two arguments?
    if (args.length != 2) {
        println(error(usage))
        sys.exit()
    }

    // parse the maze file
    val mazeFileName = args(0)
    val maze = Maze(getFileLines(mazeFileName))

    // parse the program file
    val programFilename = args(1)
    val program = FemtoParser(getFileContents(programFilename))

    // process the results of parsing
    program match {
        // Error handling: syntax errors
        case e: FemtoParser.NoSuccess => println(error(e.toString))

        // If parsing succeeded, create the bot and run it
        case FemtoParser.Success(t, _) => {
            object bot extends Picobot(maze, createAllRules(program.get)) with TextDisplay
            bot.run()
        }
    }
}

/** A string that describes how to use the program * */
def usage = "usage: sbt run <maze-file> <rules-file>"

/** Format an error message */
def error(message: String): String =
    s"${RESET}${RED}[Error] ${message}${RESET}"

/** Given a filename, get a list of the lines in the file */
def getFileLines(filename: String): List[String] =
    try {
        io.Source.fromFile(filename).getLines().toList
    } catch { // Error handling: non-existent file
        case e: java.io.FileNotFoundException => {
            println(error(e.getMessage())); sys.exit(1)
        }
    }

/** Given a filename, get the contents of the file */
def getFileContents(filename: String): String =
    try {
        io.Source.fromFile(filename).mkString
    } catch { // Error handling: non-existent file
        case e: java.io.FileNotFoundException => {
            println(error(e.getMessage())); sys.exit(1)
        }
    }