import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

class Student {
    String id;
    String name;
    String address;
    String dateOfBirth;
    int age;
    boolean isPrime;
}

class StudentThread extends Thread {
    List<Student> students = new ArrayList<>();

    public void run() {
        try {
            File inputFile = new File("student.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("student");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    Student student = new Student();
                    student.id = eElement.getElementsByTagName("id").item(0).getTextContent();
                    student.name = eElement.getElementsByTagName("name").item(0).getTextContent();
                    student.address = eElement.getElementsByTagName("address").item(0).getTextContent();
                    student.dateOfBirth = eElement.getElementsByTagName("dateOfBirth").item(0).getTextContent();
                    students.add(student);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class AgeThread extends Thread {
    List<Student> students;

    AgeThread(List<Student> students) {
        this.students = students;
    }

    public void run() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date currentDate = new Date();
        for (Student student : students) {
            try {
                Date dob = sdf.parse(student.dateOfBirth);
                long ageInMillis = currentDate.getTime() - dob.getTime();
                student.age = (int) (ageInMillis / (1000l * 60 * 60 * 24 * 365));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}

class PrimeThread extends Thread {
    List<Student> students;

    PrimeThread(List<Student> students) {
        this.students = students;
    }

    public void run() {
        for (Student student : students) {
            int sum = 0;
            for (char c : student.dateOfBirth.replaceAll("/", "").toCharArray()) {
                sum += c - '0';
            }
            student.isPrime = isPrime(sum);
        }
    }

    private boolean isPrime(int n) {
        if (n <= 1) {
            return false;
        }
        for (int i = 2; i < Math.sqrt(n); i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        StudentThread studentThread = new StudentThread();
        studentThread.start();
        studentThread.join();

        AgeThread ageThread = new AgeThread(studentThread.students);
        ageThread.start();

        PrimeThread primeThread = new PrimeThread(studentThread.students);
        primeThread.start();

        ageThread.join();
        primeThread.join();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element rootElement = doc.createElement("students");
            doc.appendChild(rootElement);

            for (Student student : studentThread.students) {
                Element studentElement = doc.createElement("student");
                rootElement.appendChild(studentElement);

                Element ageElement = doc.createElement("age");
                ageElement.appendChild(doc.createTextNode(String.valueOf(student.age)));
                studentElement.appendChild(ageElement);

                Element isPrimeElement = doc.createElement("isPrime");
                isPrimeElement.appendChild(doc.createTextNode(String.valueOf(student.isPrime)));
                studentElement.appendChild(isPrimeElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("kq.xml"));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}