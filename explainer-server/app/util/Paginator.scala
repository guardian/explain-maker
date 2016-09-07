package util

import com.gu.contentatom.thrift.Atom

case class PaginationConfig(pageNumber: Int, previousFragmentHTML: String, nextFragmentHTML: String)

object Paginator {
  def deskToQueryStringWithTrailingAmpersand(desk: Option[String]): String = {
    desk.fold("")(t => s"desk=$t&")
  }
  val pageSize: Int = 50
  def previousFragment(desk:Option[String], pageNumber: Int): String = {
    if(pageNumber>1){
      "<a class=\"pagination__link\" href=\"/?" + deskToQueryStringWithTrailingAmpersand(desk)+ "pageNumber=" + (pageNumber-1).toString() + "\">< Previous</a>"
    }else{
      ""
    }
  }
  def nextFragment(desk:Option[String], pageNumber: Int, maxPageNumber: Int): String = {
    if(pageNumber<maxPageNumber){
      "<a class=\"pagination__link\" href=\"?" + deskToQueryStringWithTrailingAmpersand(desk)+ "pageNumber=" + (pageNumber+1).toString() + "\">Next ></a>"
    }else{
      ""
    }
  }
  def maxPageNumber(numberOfExplainers: Int): Int = {
    if (numberOfExplainers % pageSize == 0){
      (numberOfExplainers.toFloat / pageSize).toInt
    }else{
      (numberOfExplainers.toFloat/ pageSize).toInt+1
    }
  }

  def selectPageExplainers(explainers: Seq[Atom], pageNumber: Int): Seq[Atom] = {
    // Here we drop pageNumber*pageSize and then keep the next pageSize elements.
    val startIndex = (pageNumber - 1) * pageSize
    explainers.slice(startIndex, startIndex + pageSize)
  }


  def getPaginationConfig(pageNumber: Int, desk: Option[String], explainersWithSorting: Seq[Atom]): PaginationConfig = {
    PaginationConfig(
      pageNumber,
      previousFragment(desk,pageNumber),
      nextFragment(desk, pageNumber, maxPageNumber(explainersWithSorting.length)))
  }
}