import com.braseiro.dnd5e.RuleCuriosityCatalog;
import java.util.HashSet;
public final class RuleCuriosityCatalogTest {
 public static void main(String[] args){
  if(RuleCuriosityCatalog.count()!=14)throw new AssertionError("bad count");
  HashSet<String> ids=new HashSet<>(); HashSet<Integer> pages=new HashSet<>();
  for(int i=0;i<RuleCuriosityCatalog.count();i++){
   RuleCuriosityCatalog.Entry e=RuleCuriosityCatalog.at(i);
   if(!RuleCuriosityCatalog.validPrintedPage(e))throw new AssertionError("page");
   if(!ids.add(e.id))throw new AssertionError("duplicate");
   if(e.title.isEmpty()||e.explanation.length()<40||e.application.length()<30)throw new AssertionError("empty lesson");
   pages.add(e.printedPage);
  }
  if(!RuleCuriosityCatalog.at(-1).id.equals(RuleCuriosityCatalog.at(13).id))throw new AssertionError("negative index");
  if(!RuleCuriosityCatalog.at(14).id.equals(RuleCuriosityCatalog.at(0).id))throw new AssertionError("wrap");
  if(pages.size()<9)throw new AssertionError("few pages");
  System.out.println("PASS catalog 14 unique entries and pagination");
 }
}
