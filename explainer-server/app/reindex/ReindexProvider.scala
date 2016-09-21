package data

import javax.inject.{Inject, Provider}
import config.Config

import com.gu.atom.publish._

class PublishedReindexerProvider @Inject() (config: Config)
  extends Provider[PublishedAtomReindexer] {
  def get() = new PublishedKinesisAtomReindexer(config.liveReindexKinesisStreamName, config.kinesisClient)
}

class PreviewReindexerProvider @Inject() (config: Config)
  extends Provider[PreviewAtomReindexer] {
  def get() = new PreviewKinesisAtomReindexer(config.previewReindexKinesisStreamName, config.kinesisClient)
}
