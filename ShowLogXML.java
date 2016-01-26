package LogXML;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.miginfocom.swing.MigLayout;

public class ShowLogXML extends JFrame {
	private static final long serialVersionUID = 1L;
	private JFrame frmMain = new JFrame();
	private static ImageIcon ICONUSERS = new ImageIcon("res/Help book 3d.png");

	private JTextArea taDisplay = new JTextArea();

	private JButton btnDisplayLog = new JButton("Display Log");
	
	public ShowLogXML() {
        super();

    // Create the Frame
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int maxX = screenSize.width - 300;
		int maxY = screenSize.height - 300;
		
		frmMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmMain.setSize(maxX, maxY);
		frmMain.setLocationRelativeTo(null);
		
		frmMain.setTitle("Friendly-Tutor(TM) Log Display (v1-1-2)");
		
//		JPanel pnlTop = new JPanel(new MigLayout("debug"));
		JPanel pnlTop = new JPanel(new MigLayout());

		btnDisplayLog.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				createLogDisplay();				
			}});
		pnlTop.add(btnDisplayLog);
		
//		JPanel pnlMain = new JPanel(new MigLayout("fill, debug"));
		JPanel pnlMain = new JPanel(new MigLayout("fill"));

		taDisplay.setEditable(false);
		taDisplay.setFont(new Font("monospaced", Font.PLAIN, 12));
		JScrollPane scrollPane = new JScrollPane(taDisplay);
		pnlMain.add(scrollPane, "grow");

//		JPanel pnlBottom = new JPanel(new MigLayout("debug"));
		JPanel pnlBottom = new JPanel(new MigLayout());

		frmMain.add(pnlTop, BorderLayout.NORTH);
		frmMain.add(pnlMain, BorderLayout.CENTER);
		frmMain.add(pnlBottom, BorderLayout.SOUTH);
		
		frmMain.setIconImage(ICONUSERS.getImage());
		
		frmMain.setVisible(true);
	}
	
	private void createLogDisplay() {
		Path pathLogFile = getTargetDir();
		if (pathLogFile == null) return;

		taDisplay.setText("");  // always clear text area before loading a new log file
		taDisplay.append(pathLogFile.toString() + System.lineSeparator());
		
		List<Record> recordList = loadLogXmlFile(pathLogFile);
		
		List<String> slistMethods = new ArrayList<String>();

		//Printing the populated log list
		for (Record record : recordList) {
			if (!slistMethods.contains(record.sMethod)) {
				slistMethods.add(record.sMethod);
			}
		}
		
		int nMethods = slistMethods.size();
		int [] iPosition = new int[nMethods];
		StringBuilder sbMethods = new StringBuilder();
		StringBuilder sb0 = new StringBuilder();
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		
		int i = 0;
		int iTotal = 0;
		for (String sTemp : slistMethods) {
			iPosition[i++] = iTotal;
			sbMethods.append(sTemp + " ");
			iTotal = iTotal + sTemp.length() + 1;
		}

		for (i = 0; i < iTotal; i++) {
			int i100 = i - (100 * (i / 100));
			sb0.append(i100 % 10);
			sb1.append(i100 / 10);
			sb2.append(i / 100);
		}
		taDisplay.append(sb0 + System.lineSeparator());
		taDisplay.append(sb1 + System.lineSeparator());
		taDisplay.append(sb2 + System.lineSeparator());
		taDisplay.append(sbMethods + System.lineSeparator());

		StringBuilder sbLine = new StringBuilder();
		
//		for (i = 0; i < nMethods; i++) {
//			taDisplay.append(sbMethods + System.lineSeparator());
//			for (int ii = 0; ii < iPosition[i]; ii++) {
//				sbLine.append(' ');
//			}
//			sbLine.append('|');
//			sbLine.append(iPosition[i]);
//			taDisplay.append(sbLine + System.lineSeparator());
//			sbLine.setLength(0);
//		}
			
		String sT0 = recordList.get(0).sMillis;
		int iEvent0millis = Integer.valueOf(sT0.substring(sT0.length() - 6));
		long lT0 = recordList.get(0).lMillis;
		System.out.println(iEvent0millis + " msec  " + calcDate(recordList.get(0).lMillis));
		
		for (Record record : recordList) {
			int iMethod = slistMethods.indexOf(record.sMethod);
			for (i = 0; i < iPosition[iMethod]; i++) {
				sbLine.append(' ');
			}
//			sbLine.append('|');

			long lDelta = record.lMillis - lT0;
			
			sbLine.append(record.sSequence + "-" + record.sThread + 
//					" " + calcDate(record.lMillis) +
					" " + lDelta + " msec" +
					" (" + 
					record.sMethod + ") " + record.sMessage);
			taDisplay.append(sbLine + System.lineSeparator());
			sbLine.setLength(0);  // reinitialize the line contents
		}
	
		taDisplay.setCaretPosition(0);  // set cursor to first line in the display			 		
	}

	private String calcDate(long millisecs) {
//	    SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss.SSS");
	    SimpleDateFormat date_format = new SimpleDateFormat("HH:mm:ss.SSS");
	    Date resultdate = new Date(millisecs);
	    return date_format.format(resultdate);
	  }

/*
 * wrap XML DOM with logger specific collection structure	
 */
	private List<Record> loadLogXmlFile(Path pathLogFile) {
		Document document = parseXmlFile(pathLogFile.toString());
		
		//Iterating through the nodes and extracting the data.
		NodeList nodeList = document.getDocumentElement().getChildNodes();

		List<Record> recordList = new ArrayList<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			//We have encountered an <log> tag.
			Node node = nodeList.item(i);
			if (node instanceof Element) {
				Record record = new Record();
				NodeList childNodes = node.getChildNodes();

				for (int j = 0; j < childNodes.getLength(); j++) {
					Node cNode = childNodes.item(j);
					//Identifying the child tag of a logging record encountered.
					if (cNode instanceof Element) {
						String content = "";
						if (cNode.getLastChild() != null) {
							content = cNode.getLastChild().getTextContent().trim();
						}
//						System.out.println("node: " + i + " Child node: " + j + " " + content);
						switch (cNode.getNodeName()) {
						case "date":
							record.sDate = content;
							break;
						case "millis":
							record.sMillis = content;
							record.lMillis = Long.valueOf(content);
							break;
						case "sequence":
							record.sSequence = content;
							break;
						case "logger":
							record.sLogger = content;
							break;
						case "level":
							record.sLevel = content;
							break;
						case "class":
							record.sClass = content;
							break;
						case "method":
							record.sMethod = content;
							break;
						case "thread":
							record.sThread = content;
							break;
						case "message":
							record.sMessage = content;
							break;
						}
					}
				}
				recordList.add(record);
				System.out.println(record.toString());
			}
		}
		return recordList;
	}
/*
 *  Ingest XML file - read entire file into DOM document - return the document
 */
	private Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
   // turn off DTD validation so don't have to have DTD file available.  Assume well formed XML.
   // if DTD validation desired, the DTD file must be on the path
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            FileInputStream is = new FileInputStream(in);
            return db.parse(new InputSource(is));
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	

	private Path getTargetDir() {
		JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "XML files", "xml");
		fileChooser.setFileFilter(filter);
		fileChooser.setApproveButtonText("Select Log file");

		Path pathTargetDir = null;
		int showOpenDialog = fileChooser.showOpenDialog(null);
		if (showOpenDialog == JFileChooser.APPROVE_OPTION) {
			pathTargetDir = fileChooser.getSelectedFile().toPath();
	         System.out.println(pathTargetDir.toString());
		} else {  // null return means file chooser action was cancelled
			System.out.println("No file selected");
		}

		return pathTargetDir;  
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ShowLogXML showLog = new ShowLogXML();
					showLog.createLogDisplay();  // immediately invoke primary method
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}
}

class Record {
	String sDate;
	String sMillis;
	long lMillis;
	String sSequence;
	String sLogger;
	String sLevel;
	String sClass;
	String sMethod;
	String sThread;
	String sMessage;
	
	@Override
	public String toString() {
		String sMsg = sDate + " " + sMillis + " " + sSequence + " " + sThread + "\n" +
			" " + sLogger + " " + sLevel + " " + sClass + " " + " " + sMethod + "\n" +
			" " + sMessage;
		return sMsg;
	}
	
}