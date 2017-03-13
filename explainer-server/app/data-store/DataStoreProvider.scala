package com.gu.atom.data

import javax.inject.{ Inject, Provider }
import com.gu.contentatom.thrift.atom.explainer._
import com.gu.contentatom.thrift._
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import config.Config

import ScanamoUtil._
import scala.reflect.classTag

import DynamoFormat._

class PublishedExplainerAtomDataStoreProvider @Inject() (config: Config) extends Provider[PublishedDataStore] {
  def get = new PublishedDynamoDataStore(config.dynamoClient, config.liveTableName)
}

class PreviewExplainerAtomDataStoreProvider @Inject() (config: Config) extends Provider[PreviewDataStore] {
  def get = new PreviewDynamoDataStore(config.dynamoClient, config.previewTableName)
}
