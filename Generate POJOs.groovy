import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.sample;"

// 連想配列に正規表現でマッピングの定義を書いていく
typeMapping = [
  (~/(?i)int/)                      : "long",
  (~/(?i)float|double|decimal|real/): "double",
  (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
  (~/(?i)date/)                     : "java.sql.Date",
  (~/(?i)time/)                     : "java.sql.Time",
  (~/(?i)/)                         : "String"
]

// 出力ディレクトリを指定させるおまじない
FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}
/**
 * generate
 * @param table テーブルのschema情報
 * @param dir 出力ディレクトリ
 * @return
 */
def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  // 当たり前ですがファイル名
  new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields) }
}

/**
 * generate
 * @param out 出力インスタンス
 * @param className クラス名
 * @param fields 各フィールド情報が入った配列
 * @return
 */
def generate(out, className, fields) {
  out.println "package $packageName"
  out.println ""
  out.println ""
  out.println "public class $className {"
  out.println ""
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "  private ${it.type} ${it.name};"
  }
  out.println ""
  fields.each() {
    out.println ""
    out.println "  public ${it.type} get${it.name.capitalize()}() {"
    out.println "    return ${it.name};"
    out.println "  }"
    out.println ""
    out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
    out.println "    this.${it.name} = ${it.name};"
    out.println "  }"
    out.println ""
  }
  out.println "}"
}

/**
 * calcFields
 * 諸々の情報を連想配列fieldsに入れている
 * @param table テーブルのschema情報
 * @return
 */
def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    // 正規表現で表した型と比較して合わせる
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                // DasUtil.isPrimary(col)でプライマリーなカラムを取り出せる
                // col.getComment()でカラムコメントを取り出せる
                 name : javaName(col.getName(), false),
                 type : typeStr,
                 annos: ""]]
  }
}

/**
 * javaName　SnakeCaseからCamelCaseにしている
 * @param str
 * @param capitalize
 * @return
 */
def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
