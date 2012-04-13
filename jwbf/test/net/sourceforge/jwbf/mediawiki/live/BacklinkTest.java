package net.sourceforge.jwbf.mediawiki.live;

import static net.sourceforge.jwbf.mediawiki.BotFactory.getMediaWikiBot;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Vector;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.actions.util.ProcessException;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.mediawiki.LiveTestFather;
import net.sourceforge.jwbf.mediawiki.VersionTestClassVerifier;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.actions.queries.BacklinkTitles;
import net.sourceforge.jwbf.mediawiki.actions.util.RedirectFilter;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;

/**
 * 
 * @author Thomas Stock
 * 
 */
@Slf4j
public class BacklinkTest extends AbstractMediaWikiBotTest {

  @ClassRule
  public static VersionTestClassVerifier classVerifier = new VersionTestClassVerifier(
      BacklinkTitles.class);

  @Rule
  public Verifier successRegister = classVerifier.getSuccessRegister(this);

  private static final String BACKLINKS = "Backlinks";
  private static final int COUNT = 60;

  protected final void doPreapare() throws ActionException, ProcessException {
    log.info("prepareing backlinks...");
    SimpleArticle a = new SimpleArticle();
    for (int i = 0; i <= COUNT; i++) {
      a.setTitle("Back" + i);
      if (i % 2 == 0) {
        a.setText("#redirect [[" + BACKLINKS + "]]");
      } else {
        a.setText("[[" + BACKLINKS + "]]");
      }
      bot.writeContent(a);
    }
    log.info("... done");
  }

  /**
   * Test backlinks.
   * 
   * @throws Exception
   *           a
   */
  @Test
  public final void backlinksWikipediaDe() throws Exception {
    String url = "http://de.wikipedia.org/w/index.php";
    LiveTestFather.assumeReachable(url);
    bot = new MediaWikiBot(url);
    BacklinkTitles is = new BacklinkTitles(bot,
        LiveTestFather.getValue("backlinks_article"));

    int i = 0;
    while (is.hasNext()) {
      is.next();
      i++;
      if (i > getIntValue("backlinks_article_count") + 1) {
        break;
      }
    }

    Assert.assertTrue("Fail: " + i + " < "
        + getIntValue("backlinks_article_count"),
        i > getIntValue("backlinks_article_count"));
  }

  @Test
  public final void backlinksMW1x15() throws Exception {

    bot = getMediaWikiBot(Version.MW1_15, true);
    Assert.assertEquals(Version.MW1_15, bot.getVersion());
    doTest();

  }

  @Test
  public final void backlinksMW1x16() throws Exception {

    bot = getMediaWikiBot(Version.MW1_16, true);
    Assert.assertEquals(Version.MW1_16, bot.getVersion());
    doTest();

  }

  @Test
  public final void backlinksMW1x18() throws Exception {

    bot = getMediaWikiBot(Version.MW1_18, true);
    Assert.assertEquals(Version.MW1_18, bot.getVersion());
    doTest();

  }

  private void doTest() throws Exception {
    doTest(RedirectFilter.all);
  }

  private void doTest(RedirectFilter rf) throws Exception {

    BacklinkTitles gbt = new BacklinkTitles(bot, BACKLINKS, rf,
        MediaWiki.NS_MAIN, MediaWiki.NS_CATEGORY);

    Vector<String> vx = new Vector<String>();
    Iterator<String> is = gbt.iterator();
    boolean notEnougth = true;
    int i = 0;
    while (is.hasNext()) {
      is.next();
      i++;
      if (i > COUNT) {
        notEnougth = false;
        break;
      }
    }
    if (notEnougth) {
      log.warn(i + " backlinks are to less ( requred for test: " + COUNT + ")");
      doPreapare();
    }
    is = gbt.iterator();
    vx.add(is.next());
    vx.add(is.next());
    vx.add(is.next());
    is = gbt.iterator();
    i = 0;
    while (is.hasNext()) {
      String buff = is.next();
      vx.remove(buff);
      i++;
      if (i > COUNT) {
        break;
      }
    }
    assertTrue("Iterator should contain: " + vx, vx.isEmpty());
    assertTrue("Fail: " + i + " < " + COUNT, i > COUNT - 1);

  }

  private static int getIntValue(final String key) throws Exception {
    return Integer.parseInt(LiveTestFather.getValue(key));
  }
}
