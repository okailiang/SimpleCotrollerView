package com.utils;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * <p>
 * Class:XmlTranstoHtml
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: USTC
 * </p>
 * 
 * @author
 * @version 1.0.0
 */
public class XmlTranstoHtmlUtil {
	/**
	 * 
	 * @Description：将xml文件转为html
	 * @param xmlFileName
	 * @param xslFileName
	 * @param htmlFileName
	 * @throws TransformerConfigurationException
	 */
	public static void Transform(String xmlFileName, String xslFileName,
			String htmlFileName) throws Exception {

		TransformerFactory tFac = TransformerFactory.newInstance();
		Source xslSource = new StreamSource(xslFileName);
		// 根据XSL文件创建准个转换对象
		Transformer t = tFac.newTransformer(xslSource);
		File xmlFile = new File(xmlFileName);
		File htmlFile = new File(htmlFileName);
		Source source = new StreamSource(xmlFile);
		Result result = new StreamResult(htmlFile);
		// 处理xml进行交换
		t.transform(source, result);

	}
}
