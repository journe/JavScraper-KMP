import json


SAMPLE_HTML = """
<html><head>
<meta property="og:title" content="FC2-PPV-1723984 透け透け体操服にローライズブルマーでハミ尻。"/>
<meta property="og:image" content="https://storage58000.contents.fc2.com/file/367/36632163/1615552591.1.jpg"/>
<meta property="og:description" content="og fallback summary"/>
</head><body>
<section class="items_article_wrapper">
  <section class="items_article_header">
    <section class="items_article_headerTitleInArea">
      <div class="items_article_headerInfo">
        <div class="items_article_softDevice"><p>販売日 : 2021/03/12</p></div>
        <p>商品ID : FC2 PPV 1723984</p>
      </div>
      <div class="items_article_MainitemThumb">
        <span><p class="items_article_info">01:01:02</p></span>
      </div>
      <section class="items_article_TagArea">
        <h3>商品タグ</h3>
        <div data-feed-container="">
          <a class="tag tagTag" data-tag="ブルマ" href="/search/?tag=%E3%83%96%E3%83%AB%E3%83%9E">ブルマ</a>
          <a class="tag tagTag" data-tag="電マ" href="/search/?tag=%E9%9B%BB%E3%83%9E">電マ</a>
        </div>
      </section>
    </section>
  </section>
  <section class="items_article_SampleImages">
    <h3>サンプル画像</h3>
    <ul class="items_article_SampleImagesArea">
      <li><a><img src="//contents-thumbnail2.fc2.com/w480/storage58000.contents.fc2.com/file/367/36632163/1615552591.2.jpg"/></a></li>
      <li><a><img src="//contents-thumbnail2.fc2.com/w480/storage58000.contents.fc2.com/file/367/36632163/1615552591.43.jpg"/></a></li>
    </ul>
  </section>
  <section class="items_article_Contents">
    <h3>商品説明</h3>
    <div>透け透け体操服からスケル乳首。</div>
  </section>
  <section class="items_article_Review">
    <section class="items_article_reviewComp"><section>平均評価 4.8</section></section>
  </section>
</section>
</body></html>
"""


MAIN_HTML_WITH_IFRAME = SAMPLE_HTML.replace(
    '<div>透け透け体操服からスケル乳首。</div>',
    '<div><iframe data-iframe="description" src="/widget/article/1723984/description?ac=demo"></iframe></div>',
)

LD_PRODUCT = {
    "@type": "Product",
    "name": "Some English Title",
    "image": {"url": "http://storage58000.contents.fc2.com/file/367/36632163/1615552591.1.jpg"},
    "aggregateRating": {"ratingValue": 4.5, "bestRating": 5, "worstRating": 1, "reviewCount": 12},
    "description": "Full description from JSON-LD.",
}

MULTILANG_HTML = f"""
<html><head>
<meta property="og:title" content="FC2-PPV-1723984 Some English Title"/>
<script type="application/ld+json">{json.dumps(LD_PRODUCT)}</script>
</head><body>
<section class="items_article_wrapper">
  <section class="items_article_header">
    <section class="items_article_headerTitleInArea">
      <div class="items_article_headerInfo">
        <div class="items_article_softDevice"><p>Release Date : 2021/03/12</p></div>
        <p>Product ID : FC2 PPV 1723984</p>
      </div>
      <div class="items_article_MainitemThumb">
        <span><p class="items_article_info">01:01:02</p></span>
      </div>
      <section class="items_article_TagArea">
        <h3>Tags</h3>
        <a class="tag tagTag" data-tag="ブルマ" href="/search/?tag=x">ブルマ</a>
      </section>
    </section>
  </section>
</section>
</body></html>
"""



NO_LD_MULTILANG_HTML = """
<html><head>
<meta property="og:title" content="FC2-PPV-1723984 Some English Title"/>
</head><body>
<section class="items_article_wrapper">
  <section class="items_article_header">
    <section class="items_article_headerTitleInArea">
      <div class="items_article_headerInfo">
        <div class="items_article_softDevice"><p>Release Date : 2021/03/12</p></div>
        <p>Product ID : FC2 PPV 1723984</p>
      </div>
      <div class="items_article_MainitemThumb">
        <span><p class="items_article_info">01:01:02</p></span>
      </div>
      <section class="items_article_Review">
        <section class="items_article_reviewComp">
          <section>Average Rating: 4.2</section>
        </section>
      </section>
    </section>
  </section>
</section>
</body></html>
"""
WIDGET_HTML = """
<html><body>
<div>
透け透け体操服からスケル乳首。<br/>
<span>MC5kY2Y2NjU5Yi4xYTRlNTAuNmE5MDI0ZGU=</span><br/>
そんな彼女は、私、あんまらない可愛くないけど。<br/>
<div>パンファイ① 極上の美脚、至極のパンスト！ 800 pt カートに入れる</div>
</div>
</body></html>
"""


MINIMAL_ARCHIVE_HTML = """
<html><head><meta property="og:title" content="FC2-PPV-1723984 Archive">
<meta property="og:image" content="https://example.test/cover.jpg"></head>
<body><section class="items_article_Contents">
<iframe data-iframe="description" src="https://example.test/widget"></iframe>
</section></body></html>
"""
