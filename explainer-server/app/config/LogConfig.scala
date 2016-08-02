package config


import ch.qos.logback.classic.{Logger => LogbackLogger}
import net.logstash.logback.layout.LogstashLayout
import org.slf4j.{LoggerFactory, Logger => SLFLogger}
import com.gu.logback.appender.kinesis.KinesisAppender
import play.api.Logger
import javax.inject.Inject

import ch.qos.logback.classic.spi.ILoggingEvent

import ch.qos.logback.core.{Appender, LayoutBase}
import com.google.inject.AbstractModule


class LogConfig @Inject() (config: Config) extends AbstractModule {

  val rootLogger = LoggerFactory.getLogger(SLFLogger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]

  def configure {
    rootLogger.info("bootstrapping kinesis appender if configured correctly")
    val stack = config.stack
    val app = config.app
    val stage = config.stage

    Logger.info(s"bootstrapping kinesis appender with $stack -> $app -> $stage -> ${config.elkKinesisStream}")
    val context = rootLogger.getLoggerContext

    val layout = new LogstashLayout()
    layout.setContext(context)
    layout.setCustomFields(s"""{"stack":"$stack","app":"$app","stage":"$stage"}""")
    layout.start()

    val appender = new KinesisAppender()
    appender.setBufferSize(1000)
    appender.setRegion(config.region.toString)
    appender.setStreamName(config.elkKinesisStream)
    appender.setContext(context)
    appender.setLayout(layout.asInstanceOf[LayoutBase[Nothing]])
    appender.setCredentialsProvider(config.awsCredentialsprovider)

    appender.start()

    rootLogger.addAppender(appender.asInstanceOf[Appender[ILoggingEvent]])
    rootLogger.info("Configured kinesis appender")
  }
  if (config.elkLoggingEnabled) {
    configure
  }

}