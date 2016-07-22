package data

import javax.inject.{Inject, Provider}
import config.Config

import com.gu.atom.publish._

class LiveAtomPublisherProvider @Inject() (config: Config)
  extends Provider[LiveAtomPublisher] {
  def get() = new LiveKinesisAtomPublisher(config.liveKinesisStreamName, config.kinesisClient)
}

class PreviewAtomPublisherProvider @Inject() (config: Config)
  extends Provider[PreviewAtomPublisher] {
  def get() = new PreviewKinesisAtomPublisher(config.previewKinesisStreamName, config.kinesisClient)
}
