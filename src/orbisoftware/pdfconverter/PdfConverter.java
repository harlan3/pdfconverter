
package orbisoftware.pdfconverter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.PDFRenderer;

import jargs.gnu.CmdLineParser;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PdfConverter {

	private void printUsage() {

		System.out.println("Usage: PdfConverter [OPTION]...");
		System.out.println("Generate embedded text into a pdf document.");
		System.out.println();
		System.out.println("   -i, --input        Pdf input file");
		System.out.println("   -o, --output       Pdf output file");
		System.out.println("   -h, --help         Show this help message");

	}

	public void deleteDirectory(File directory) {

		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : files) {
					deleteDirectory(file); // Recursively delete subfiles/subdirectories
				}
			}
		}
		directory.delete(); // Delete the file or empty directory
	}

	public int extractImages(String pdfFilePath, String outputDirectory) {

		int numberPages = 0;

		try {
			PDDocument document = PDDocument.load(new File(pdfFilePath));
			PDFRenderer pdfRenderer = new PDFRenderer(document);

			File outputDir = new File(outputDirectory);
			deleteDirectory(outputDir);

			if (!outputDir.exists())
				outputDir.mkdirs();

			numberPages = document.getNumberOfPages();

			for (int i = 0; i < numberPages; i++) {

				BufferedImage image = pdfRenderer.renderImageWithDPI(i, 300);
				String fileName = outputDirectory + "image_" + i + ".png";

				ImageIO.write(image, "png", new File(fileName));
				System.out.println("Extracted image: " + "image_" + i + ".png");
			}
			System.out.println("Image extraction complete.");

		} catch (IOException e) {
			e.printStackTrace();
		}

		return numberPages;
	}

	public void extractText(int numPages, String outputDirectory) {

		try {
			for (int i = 0; i < numPages; i++) {

				ProcessBuilder processBuilder = new ProcessBuilder();
				processBuilder.command("tesseract", outputDirectory + "image_" + i + ".png",
						outputDirectory + "text_" + i);

				System.out.println("Extracted text: " + "text_" + i + ".txt");
				Process process = processBuilder.start();
				process.waitFor();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Text extraction complete.");
	}

	public void insertText(int numberPages, String pdfFilePath, String textInputDirectory, String outputPdfPath) {

		try {
			PDDocument document = PDDocument.load(new File(pdfFilePath));

			for (int i = 0; i < numberPages; i++) {

				PDPage page = document.getPage(i);
				PDPageContentStream contentStream = new PDPageContentStream(document, page,
						PDPageContentStream.AppendMode.APPEND, false);

				// Calculate text position
				float pageHeight = page.getMediaBox().getHeight();
				float textY = pageHeight - 250;
				float textX = 50;

				contentStream.beginText();
				contentStream.setFont(PDType1Font.HELVETICA, 1.0f);
				contentStream.appendRawCommands("3 Tr\n"); // make text invisible

				List<String> lines = Files
						.readAllLines(Paths.get(textInputDirectory + File.separator + "text_" + i + ".txt"));
				String singleLine = String.join("", lines);

				System.out.println("Embedded text: " + "text_" + i + ".txt");

				contentStream.newLineAtOffset(textX, textY);
				contentStream.showText(singleLine);
				contentStream.endText();
				contentStream.close();
			}
			System.out.println("Embedded text complete.");

			document.save(outputPdfPath);
			document.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		PdfConverter pdfConverter = new PdfConverter();
		CmdLineParser parser = new CmdLineParser();
		String outputDirectory = "output_images" + File.separator;

		CmdLineParser.Option inputOption = parser.addStringOption('i', "input");
		CmdLineParser.Option outputOption = parser.addStringOption('o', "output");
		CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.out.println(e.getMessage());
			pdfConverter.printUsage();
			System.exit(0);
		}

		String inputValue = (String) parser.getOptionValue(inputOption);
		String outputValue = (String) parser.getOptionValue(outputOption);
		Boolean helpValue = (Boolean) parser.getOptionValue(helpOption);

		if ((helpValue != null) || (inputValue == null) || (outputValue == null)) {
			pdfConverter.printUsage();
			System.exit(0);
		}

		int numberPages = pdfConverter.extractImages(inputValue, outputDirectory);

		pdfConverter.extractText(numberPages, outputDirectory);
		pdfConverter.insertText(numberPages, inputValue, outputDirectory, outputValue);

		System.out.println("Conversion complete.");

		File outputDir = new File(outputDirectory);
		pdfConverter.deleteDirectory(outputDir);
	}
}