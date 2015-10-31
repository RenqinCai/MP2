package analyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import structures.Token;

public class WriteReadExcel{
	
	public WriteReadExcel(){
		
	}
	
	public void writeExcel(String fileName, String fileName2,
		HashMap<String, Token> m_statsArg) {
		try {
			FileWriter writer = new FileWriter(fileName);
			FileWriter writer2 = new FileWriter(fileName2);

			for (String key : m_statsArg.keySet()) {

				writer.append(key);
				writer.append('\n');

				double TTF = m_statsArg.get(key).getValue();

				writer2.append(Double.toString(TTF));
				writer2.append('\n');
			}

			writer.flush();
			writer.close();

			writer2.flush();
			writer2.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
//
//		Workbook wb = new HSSFWorkbook();
//		Sheet sheet1 = wb.createSheet("TTF1");
//		Sheet sheet2 = wb.createSheet("TTF2");
//		Sheet sheet3 = wb.createSheet("TTF3");
//		
//		int rowNum = 0;
//		int i = 0;
//
//		for (String key : m_statsArg.keySet()) {
//			if (i / 60000 == 0) {
//
//				Row row = sheet1.createRow(rowNum++);
//				double TTF = m_statsArg.get(key).getValue();
//
//				Cell cell1 = row.createCell(0);
//				cell1.setCellValue(key);
//				Cell cell2 = row.createCell(1);
//				cell2.setCellValue(TTF);
//
//				i++;
//			}
//			else {
//				if (i/60000 == 1){
//
//					rowNum = 0;
//					Row row = sheet2.createRow(rowNum++);
//					double TTF = m_statsArg.get(key).getValue();
//	
//					Cell cell1 = row.createCell(0);
//					cell1.setCellValue(key);
//					Cell cell2 = row.createCell(1);
//					cell2.setCellValue(TTF);
//	
//					i++;
//				}
//				else {
//
//					rowNum = 0;
//					Row row = sheet3.createRow(rowNum++);
//					double TTF = m_statsArg.get(key).getValue();
//			
//					Cell cell1 = row.createCell(0);
//					cell1.setCellValue(key);
//					Cell cell2 = row.createCell(1);
//					cell2.setCellValue(TTF);
//			
//					i++;
//				}
//			}
//		}
//
//		try {
//			File exlFile = new File(inputFile);
//		
//			FileOutputStream out = new FileOutputStream(exlFile);
//			wb.write(out);
//			out.close();
//			System.out.println("Excel written successfully..");
//			     
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
