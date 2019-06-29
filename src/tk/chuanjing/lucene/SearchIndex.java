package tk.chuanjing.lucene;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 * @author ChuanJing
 * @date 2017年12月30日 上午12:37:13
 * @version 1.0
 */
public class SearchIndex {
	
	private String pathname = "index";//存放索引的位置
	
	private IndexSearcher getIndexSearcher() throws Exception {
		//====1指定索引库存放的位置
		Directory directory = FSDirectory.open(new File(pathname));
		
		//====2使用IndexReader对象打开索引库
		IndexReader indexReader = DirectoryReader.open(directory);
		
		//====3创建一个IndexSearcher对象，构造方法需要一个indexReader对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		return indexSearcher;
	}
	
	private void printResult(IndexSearcher indexSearcher, Query query) throws Exception {
		//====5查询索引库
		//参数1：查询条件
		//参数2：查询结果返回的最大值
		TopDocs topDocs = indexSearcher.search(query, 100);
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		System.out.println("查询结果总记录数："  + topDocs.totalHits);
		
		//====6遍历查询结果并打印
		for (ScoreDoc scoreDoc : scoreDocs) {
			//取文档id
			int docId = scoreDoc.doc;
			//通过id查询文档对象
			Document document = indexSearcher.doc(docId);
			//取属性
			System.out.println(document.get("name"));
			System.out.println(document.get("size"));
			System.out.println(document.get("content"));
			System.out.println(document.get("path"));
		}
		
		//====7关闭IndexReader对象
		indexSearcher.getIndexReader().close();
	}
	
	@Test
	public void testMatchAllDocsQuery() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		
		//====4创建一个查询对象,需要指定查询域及要查询的关键字。
		Query query = new MatchAllDocsQuery();
		System.out.println(query);
		
		printResult(indexSearcher, query);
	}
	
	@Test
	public void testNumericRangeQuery() throws Exception {
		//创建一个数值范围查询对象
		//参数1：要查询的域
		//参数2：最小值
		//参数3：最大值
		//参数4：是否包含最小值
		//参数5：是否包含最大值
		Query query = NumericRangeQuery.newLongRange("size", 1000l, 10000l, false, true);
		System.out.println(query);
		
		//打印结果
		printResult(getIndexSearcher(), query);
	}
	
	@Test
	public void testBooleanQuery() throws Exception {
		//创建一个BooleanQuery对象
		BooleanQuery query = new BooleanQuery();
		
		//创建子查询，文件大于1000小于10000
		Query query1 = NumericRangeQuery.newLongRange("size", 1000l, 10000l, true, true);
		//创建子查询，文件名中包含mybatis关键字
		Query query2 = new TermQuery(new Term("name", "mybatis"));
		
		//添加到BooleanQuery对象中
		query.add(query1,Occur.MUST);
//		query.add(query2,Occur.MUST);
//		query.add(query2,Occur.MUST_NOT);
		query.add(query2,Occur.SHOULD);
		System.out.println(query);
		
		//执行查询
		printResult(getIndexSearcher(), query);
	}
	
	/**
	 * 重要：在大多数情况下，用户的输入不一定是一个词条，所以我们
	 * 需要对用户的输入进行分词，将输入编程多个词条之后进行查询。
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryParser() throws Exception {
		//创建一个QueryParser对象。
		//参数1：默认搜索域
		//参数2：分析器对象。
		QueryParser queryParser = new QueryParser("content", new IKAnalyzer());
		
		//调用parse方法可以获得一个Query对象
		//参数：要查询的内容，可以是一句话。先分词在查询
		Query query = queryParser.parse("mybatis is a apache project");
//		Query query = queryParser.parse("+name:lucene +name:apache");//测试查询语法
//		Query query = queryParser.parse("name:lucene AND name:apache");//测试查询语法
		System.out.println(query);
		printResult(getIndexSearcher(), query);
	}
	
	/**
	 * 重要：有时候业务会提供多个字段供用户选择，店铺，商家，旺旺。
	 * @throws Exception
	 */
	@Test
	public void testMultiFileQueryParser() throws Exception {
		//指定默认搜索域
		String[] fields = {"name", "content"};
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, new IKAnalyzer());
		Query query = queryParser.parse("mybatis is a apache project");
		System.out.println(query);
		printResult(getIndexSearcher(), query);
	}
}
