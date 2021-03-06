package play.api

import org.slf4j.{ LoggerFactory, Logger => Slf4jLogger }

/** Typical logger interface. */
trait LoggerLike {

  /** The underlying SLF4J Logger. */
  val logger: Slf4jLogger

  /** `true` if the logger instance is enabled for the `TRACE` level. */
  lazy val isTraceEnabled = logger.isTraceEnabled

  /** `true` if the logger instance is enabled for the `DEBUG` level. */
  lazy val isDebugEnabled = logger.isDebugEnabled

  /** `true` if the logger instance is enabled for the `INFO` level. */
  lazy val isInfoEnabled = logger.isInfoEnabled

  /** `true` if the logger instance is enabled for the `WARN` level. */
  lazy val isWarnEnabled = logger.isWarnEnabled

  /** `true` if the logger instance is enabled for the `ERROR` level. */
  lazy val isErrorEnabled = logger.isWarnEnabled

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   */
  def trace(message: String) { logger.trace(message) }

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def trace(message: String, error: Throwable) { logger.trace(message, error) }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   */
  def debug(message: String) { logger.debug(message) }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def debug(message: String, error: Throwable) { logger.debug(message, error) }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   */
  def info(message: String) { logger.info(message) }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def info(message: String, error: Throwable) { logger.info(message, error) }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   */
  def warn(message: String) { logger.warn(message) }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def warn(message: String, error: Throwable) { logger.warn(message, error) }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   */
  def error(message: String) { logger.error(message) }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def error(message: String, error: Throwable) { logger.error(message, error) }

}

/**
 * A Play logger.
 *
 * @param logger the underlying SL4FJ logger
 */
class Logger(val logger: Slf4jLogger) extends LoggerLike

/**
 * High-level API for logging operations.
 *
 * For example, logging with the default application logger:
 * {{{
 * Logger.info("Hello!")
 * }}}
 *
 * Logging with a custom logger:
 * {{{
 * Logger("my.logger").info("Hello!")
 * }}}
 *
 */
object Logger extends LoggerLike {

  /**
   * The 'application' logger.
   */
  val logger = LoggerFactory.getLogger("application")

  /**
   * Obtains a logger instance.
   *
   * @param name the name of the logger
   * @return a logger
   */
  def apply(name: String): Logger = new Logger(LoggerFactory.getLogger(name))

  /**
   * Obtains a logger instance.
   *
   * @param clazz a class whose name will be used as logger name
   * @return a logger
   */
  def apply[T](clazz: Class[T]): Logger = new Logger(LoggerFactory.getLogger(clazz))

  /**
   * Reconfigures the underlying logback infrastructure.
   *
   * @param configFile the logback configuration file
   * @param properties these properties will be added to the logger context (for example `application.home`)
   * @see http://logback.qos.ch/
   */
  def configure(configFile: java.net.URL, properties: Map[String, String] = Map.empty, levels: Map[String, ch.qos.logback.classic.Level] = Map.empty) {

    // Redirect JUL -> SL4FJ
    {
      import org.slf4j.bridge._
      import java.util.logging._

      Option(java.util.logging.Logger.getLogger("")).map { root =>
        root.setLevel(Level.FINEST)
        root.getHandlers.foreach(root.removeHandler(_))
      }

      SLF4JBridgeHandler.install()
    }

    // Configure logback
    {
      import org.slf4j._

      import ch.qos.logback.classic.joran._
      import ch.qos.logback.core.util._
      import ch.qos.logback.classic._

      try {
        val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
        val configurator = new JoranConfigurator
        configurator.setContext(ctx)
        ctx.reset
        properties.foreach {
          case (name, value) => ctx.putProperty(name, value)
        }
        configurator.doConfigure(configFile)
        levels.foreach {
          case (logger, level) => ctx.getLogger(logger).setLevel(level)
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(ctx)
      } catch {
        case _ =>
      }

    }

  }

  import ch.qos.logback.classic._
  import ch.qos.logback.classic.spi._
  import ch.qos.logback.classic.pattern._

  /**
   * A logback converter generating colored, lower-case level names.
   *
   * Used for example as:
   * {{{
   * %coloredLevel %logger{15} - %message%n%xException{5}
   * }}}
   */
  class ColoredLevel extends ClassicConverter {

    import play.console.Colors

    def convert(event: ILoggingEvent) = {
      event.getLevel match {
        case Level.TRACE => "[" + Colors.blue("trace") + "]"
        case Level.DEBUG => "[" + Colors.cyan("debug") + "]"
        case Level.INFO => "[" + Colors.white("info") + "]"
        case Level.WARN => "[" + Colors.yellow("warn") + "]"
        case Level.ERROR => "[" + Colors.red("error") + "]"
      }
    }

  }

}

