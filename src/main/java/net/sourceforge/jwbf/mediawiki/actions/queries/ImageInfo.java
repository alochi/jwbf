package net.sourceforge.jwbf.mediawiki.actions.queries;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jwbf.JWBF;
import net.sourceforge.jwbf.core.RequestBuilder;
import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.core.actions.util.ProcessException;
import net.sourceforge.jwbf.mediawiki.ApiRequestBuilder;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.actions.util.MWAction;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.commons.lang.math.NumberUtils;
import org.jdom.Element;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Action to receive the full address of an image. Like "Img.gif" to "http://wikihost.tld/w/images/x/y/Img.gif".
 * 
 * @author Thomas Stock
 */
@Slf4j
public class ImageInfo extends MWAction {
  private static final Map<String, String> EMPTY_STRING_MAP = Collections.emptyMap();

  public static final String WIDTH = "iiurlwidth";
  public static final String HEIGHT = "iiurlheight";

  private String urlOfImage = "";
  private Get msg;
  private final MediaWikiBot bot;
  private boolean selfEx = true;
  private final Map<String, String> map = Maps.newHashMap();

  final private String name;

  /**
   * Get an absolute url to an image.
   * 
   * @param name
   *          of, like "Test.gif"
   */
  public ImageInfo(MediaWikiBot bot, String name) {
    this(bot, name, EMPTY_STRING_MAP);
  }

  public ImageInfo(MediaWikiBot bot, String name, Map<String, String> params) {
    this.bot = bot;
    this.name = name;
    map.putAll(params);
    prepareMsg(name);
  }

  /**
   * TODO change params to a map
   */
  public ImageInfo(MediaWikiBot bot, String name, String[][] params) {
    this.bot = bot;
    this.name = name;
    if (params != null) {
      for (String[] param : params) {
        if (param.length == 2) {
          String key = param[0];
          String value = param[1];
          if (key != null && value != null)
            map.put(key, value);
        }
      }
    }
    prepareMsg(name);
  }

  private void prepareMsg(String name) {

    RequestBuilder requestBuilder = new ApiRequestBuilder() //
        .action("query") //
        .formatXml() //
        .param("iiprop", "url") //
        .param("prop", "imageinfo") //
    ;

    int width = NumberUtils.toInt(map.get(WIDTH));
    if (width > 0) {
      requestBuilder.param(WIDTH, width);
    }

    int height = NumberUtils.toInt(map.get(HEIGHT));
    if (height > 0) {
      requestBuilder.param(HEIGHT, height);
    }
    if (bot.getVersion().greaterEqThen(Version.MW1_15)) {
      requestBuilder.param("titles", "File:" + MediaWiki.encode(name));
    } else {
      requestBuilder.param("titles", "Image:" + MediaWiki.encode(name));
    }
    msg = requestBuilder.buildGet();
  }

  /**
   * @return position like "http://server.tld/path/to/Test.gif"
   */
  public String getUrlAsString() {
    return getUrl().toExternalForm();
  }

  public URL getUrl() {
    try {
      selfEx = false;
      bot.performAction(this);
    } catch (ProcessException e) {
      String exceptionMsg = "no url for image with name \"" + name + "\"";
      throw new ProcessException(exceptionMsg);
    } finally {
      selfEx = true;
    }

    String url = Preconditions.checkNotNull(urlOfImage, "imate url is null");

    return JWBF.newURL(url);
  }

  /**
   * {@inheritDoc}
   * 
   * @deprecated see super
   */
  @Deprecated
  @Override
  public boolean isSelfExecuter() {
    return selfEx;
  }

  public BufferedImage getAsImage() throws IOException {
    return ImageIO.read(getUrl());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String processAllReturningText(String s) {
    findUrlOfImage(s);
    return "";
  }

  @SuppressWarnings("unchecked")
  private void findContent(final Element root) {

    Iterator<Element> el = root.getChildren().iterator();
    while (el.hasNext()) {
      Element element = el.next();
      if (element.getQualifiedName().equalsIgnoreCase("ii")) {
        urlOfImage = element.getAttributeValue("url");

        return;
      } else {
        findContent(element);
      }
    }

  }

  private void findUrlOfImage(String s) {
    Element root = getRootElementWithError(s);
    findContent(root);
    if (urlOfImage.length() < 1) {
      throw new ProcessException("Could not find this image " + s);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpAction getNextMessage() {
    return msg;
  }
}
