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

class PublishedExplainerAtomDataStoreProvider @Inject() (config: Config)
  extends Provider[PublishedDataStore] {
  def get = new PublishedDynamoDataStore[ExplainerAtom](config.dynamoClient, config.liveTableName) {
    def fromAtomData = { case AtomData.Explainer(data) => data }
    def toAtomData(data: ExplainerAtom) = AtomData.Explainer(data)
  }
}

class PreviewExplainerAtomDataStoreProvider @Inject() (config: Config)
  extends Provider[PreviewDataStore] {
  def get = new PreviewDynamoDataStore[ExplainerAtom](config.dynamoClient, config.previewTableName) {
    def fromAtomData = { case AtomData.Explainer(data) => data }
    def toAtomData(data: ExplainerAtom) = AtomData.Explainer(data)
  }
}
