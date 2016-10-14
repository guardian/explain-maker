package util

import com.gu.contentatom.thrift.Atom

case class PaginationConfig(pageNumber: Int, totalPages: Int)

object Paginator {

  def maxPageNumber(numberOfExplainers: Int, pageSize:Int): Int = {
    if (numberOfExplainers % pageSize == 0){
      (numberOfExplainers.toFloat / pageSize).toInt
    }else{
      (numberOfExplainers.toFloat/ pageSize).toInt+1
    }
  }

  def selectPageExplainers(explainers: Seq[Atom], pageNumber: Int, pageSize:Int): Seq[Atom] = {
    // Here we drop pageNumber*pageSize and then keep the next pageSize elements.
    val startIndex = (pageNumber - 1) * pageSize
    explainers.slice(startIndex, startIndex + pageSize)
  }
}