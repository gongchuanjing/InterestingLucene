package tk.chuanjing.lucene;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 * @author ChuanJing
 * @date 2017年12月29日 下午5:11:32
 * @version 1.0
 *
 * 演示创建索引；演示搜索；查看不同分析器的分词效果
 */
public class LuceneFirst {
	
	//private String pathname = "E:\\Develop\\temp\\LuceneIndex";
	private String pathname = "index";//存放索引的位置
	
	/**
	 * 创建索引
	 * document--->分词--->倒排--->写入到索引库----介质(硬盘或者内存)
	 * 
	 * @throws Exception
	 */
	@Test
	public void createIndex() throws Exception {
		//====1、指定索引库存放的位置，可以是内存也可以是磁盘
		//索引库保存到内存中，一般不用
		//Directory directory = new RAMDirectory();
		//保存到磁盘上
		Directory directory = FSDirectory.open(new File(pathname));
		
		//====2、创建一个IndexWriter对象。需要一个分析器对象。
		//Analyzer analyzer = new StandardAnalyzer();
		Analyzer analyzer = new IKAnalyzer();
		//参数1：lucene的版本号，第二个参数：分析器对象
		IndexWriterConfig conf = new IndexWriterConfig(Version.LATEST, analyzer);
		//参数1：索引库存放的路径 参数2：配置信息，其中包含分析器对象
		IndexWriter indexWriter = new IndexWriter(directory, conf);
		
		//====3、获得原始文档，使用io流读取文本文件
		File docPath = new File("searchSource");//searchSource文件夹在项目根目录下
		for (File f : docPath.listFiles()) {
			//取文件名
			String fileName = f.getName();
			//取文件路径
			String filePath = f.getPath();
			//文件的内容
			String fileContent = FileUtils.readFileToString(f);
			//文件的大小
			long fileSize = FileUtils.sizeOf(f);
			
			//====4、创建文档对象
			Document document = new Document();
			
			/*
			 * 创建域
			 * 一个文档中，有很多字段，想通过name进行查询，这个字段需要被indexed。
			 * 如果一个想创建索引，有两种选择，一种不被分词就是完全匹配，一种是分词。比如：TextField是要被分词的，而StringField是不被分词的
			 */
			//参数1：域的名称 参数2：域的内容 参数3：是否存储
			TextField fileNameField = new TextField("name", fileName, Store.YES);
			StoredField filePathField = new StoredField("path", filePath);
			TextField fileContentField = new TextField("content", fileContent, Store.NO);
			LongField fileSizeField = new LongField("size", fileSize, Store.YES);
			//fileContentField.setBoost(10);//激励因子，设置权重
			
			//====5、向文档中添加域
			document.add(fileNameField);
			document.add(filePathField);
			document.add(fileContentField);
			document.add(fileSizeField);
			
			//====6、把文档对象写入索引库
			indexWriter.addDocument(document);
		}
		
		// commit将内存中等待的数据提交到硬盘创建索引。
		// indexWriter.commit();
		
		//====7、关闭IndexWriter对象
		indexWriter.close();
		
		/*
		if(男的){
			System.out.println("我今天休息");
			
		}else if(女的){
			System.out.println("我现在有点忙");
			
		}else if(漂亮女孩){
			System.out.println("我在316");
			
		}else if(田姐的学生 && 漂亮的女孩){
			System.out.println("你在哪？我去找你");
			
		}else {
			System.out.println("其实根本不需要这个else，但是被小田姐的美貌所打动，加上吧，反正不管加不加，都得帮她");
		}
		*/
		
	}
	
	/**
	 * 搜索
	 * @throws Exception
	 */
	@Test
	public void searchIndex() throws Exception {
		//====1指定索引库存放的位置
		Directory directory = FSDirectory.open(new File(pathname));
		
		//====2使用IndexReader对象打开索引库
		IndexReader indexReader = DirectoryReader.open(directory);
		
		//====3创建一个IndexSearcher对象，构造方法需要一个indexReader对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		//====4创建一个查询对象,需要指定查询域及要查询的关键字。
		//term的参数1：要搜索的域
		//参数2：搜索的关键字
		//Query query = new TermQuery(new Term("name", "spring"));
		Query query = new TermQuery(new Term("content", "全文"));
		
		//参数1：查询条件
		//参数2：查询结果返回的最大值
		TopDocs topDocs = indexSearcher.search(query, 10);
		
		//====5取查询结果
		//取查询结果总记录数
		System.out.println("查询结果总记录数："  + topDocs.totalHits);
		
		//====6遍历查询结果并打印
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			//取文档id
			int id = scoreDoc.doc;
			System.out.println("维护在lucene内部的文档编号:"+id);
			System.out.println("当前文档的得分:"+scoreDoc.score);
			
			//从索引库中取文档对象
			Document document = indexSearcher.doc(id);
			//取属性
			System.out.println(document.get("name"));
			System.out.println(document.get("size"));
			System.out.println(document.get("content"));
			System.out.println(document.get("path"));
		}
		
		//====7关闭IndexReader对象
		indexReader.close();
	}
	
	//查看分析器的分词效果
	@Test
	public void testAnanlyzer() throws Exception {
		//创建一个分析器对象
//		Analyzer analyzer = new StandardAnalyzer();
//		Analyzer analyzer = new CJKAnalyzer();
//		Analyzer analyzer = new SmartChineseAnalyzer();
		Analyzer analyzer = new IKAnalyzer();//好用的中文分词器
		
		//从分析器对象中获得tokenStream对象
		//参数1：域的名称，可以为null或者""
		//参数2：要分析的文本内容
//		TokenStream tokenStream = analyzer.tokenStream("", "The Spring Framework provides a comprehensive programming and configuration model");
		TokenStream tokenStream = analyzer.tokenStream("", "数据库中存储的数据是高富帅结构化数据，即行数据java，可以用二维表结构来逻辑表达实现的数据。");
		
		//设置一个引用，引用可以有多重类型，可以时候关键词的引用、偏移量的引用
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		//偏移量
		OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
		
		//调用tokenStream的reset方法
		tokenStream.reset();
		
		//使用while循环变量单词列表
		while (tokenStream.incrementToken()) {
			System.out.println("----start->" + offsetAttribute.startOffset());
			//打印单词
			System.out.println(charTermAttribute);
			System.out.println("----end->" + offsetAttribute.endOffset());
		}
		
		//关闭tokenStream
		tokenStream.close();
	}
}
