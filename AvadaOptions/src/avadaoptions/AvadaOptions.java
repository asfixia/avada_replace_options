package avadaoptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class AvadaOptions {

    public static class FromToString {
        private ArrayList<String> fromStr = new ArrayList<>();
        private ArrayList<String> toStr = new ArrayList<>();
        
        public void add(String from, String to) {
            fromStr.add(from);
            toStr.add(to);
        }
        
        public int size() {
            return fromStr.size();
        }
        
        public String doReplace(String content) {
            for (int iReplace = 0, replaceCount = fromStr.size(); iReplace < replaceCount; iReplace++) {
                content = content.replace(fromStr.get(iReplace), toStr.get(iReplace));
                System.out.println("\n\n:\n" + fromStr.get(iReplace) + "\n:\n" + toStr.get(iReplace) + "\n");
            }
            return content;
        }
    }
    
    public static class RegexUtils {

        private final static String escapedDelimiterTo = "@#@#@___@#@#@";
        private final static String escapedDelimiterFrom = "''";

        //Remove o double quote para nao trapalhar no regex //Usando "'((''|[^'])*)'" causa estouro de pilha.
        public static String removeDoubleQuote(String content) {
            return content.replaceAll(escapedDelimiterFrom, escapedDelimiterTo);
        }

        //Volta com o double Quote
        public static String reAddDoubleQuote(String content) {
            return content.replaceAll(escapedDelimiterTo, escapedDelimiterFrom);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String tempDir = System.getenv().get("TEMP");
        String dbFilePath = tempDir + "/db.sql";
        String newdbFilePath = tempDir + "/db_new.sql";
        String oldName = "http://csr.ufmg.br/inteligencia_territorial";
        String newName = "http://inteligenciaterritorial.org";
        replaceFileContents(dbFilePath, newName, oldName, newdbFilePath);
        System.out.println("Printed Content to File: " + newdbFilePath);
    }
    
    private static void replaceFileContents(String dbFilePath, String newName, String oldName, String newdbFilePath) throws NumberFormatException, IOException {
        System.out.println("Reading from (UTF-8): " + dbFilePath);
        int lenDifference = newName.length() - oldName.length();
        String fileContent = FileUtils.readFileToString(new File(dbFilePath), Charset.forName("UTF-8"));
        
        fileContent = RegexUtils.removeDoubleQuote(fileContent);
        final String stringContentsRegex = "'([^']+)'"; //Causava estouro de pilha "'((''|[^'])*)'"; Então substitui a ocorrencia com escape
        final Matcher stringMatcher = Pattern.compile(stringContentsRegex).matcher(fileContent);
        FromToString replaces = new FromToString();
        while (stringMatcher.find()) { //Para cara string do sql.
            String optionRegex = "s:([\\d]+):";
            String optionContent = stringMatcher.group(1);
            //Divide as strings a cada s:NUMERO:
            ArrayList<String> themeOptions = new ArrayList<>(Arrays.asList(optionContent.split(optionRegex)));
            //Pega o valor de cada s:NUMERO:
            final Matcher optionMatcher = Pattern.compile(optionRegex).matcher(optionContent);
            StringBuilder optionBuffer = new StringBuilder();
            int iOption = 0;
            //#FIXME se a optionContent começar com s:NUMERO: pode n ter conteúdo antes e ai buga tudo.
            String stringOptionBefore = themeOptions.get(iOption++); //Conteúdo antes da primeira string. (.*)s:NUMERO:
            //Para cada opção do avada na string
            for (int qntOptions = themeOptions.size();
                    optionMatcher.find() && iOption < qntOptions; iOption++) {
                String curOption = themeOptions.get(iOption);
                int curOptionSize = Integer.parseInt(optionMatcher.group(1));
                int curOccurrencesCount = StringUtils.countMatches(curOption, oldName);
                int newOptionSize = curOptionSize + (curOccurrencesCount * lenDifference);
                optionBuffer.append("s:")
                        .append(newOptionSize)
                        .append(":")
                        .append(curOption.replaceAll(Pattern.quote(oldName), newName));
            }
            if (optionBuffer.length() > 0) {
                String toReplace = stringOptionBefore + optionBuffer.toString();
                String fromReplace = optionContent;
                if (toReplace.equals(fromReplace)) continue; //So fazer replace qnd o conteudo for diferente
                replaces.add("'" +  fromReplace+ "'", "'" + toReplace + "'");
            }
        }
        String contentAfterReplace = replaces.doReplace(fileContent);
        FileUtils.writeStringToFile(new File(newdbFilePath),
                RegexUtils.reAddDoubleQuote(
                        contentAfterReplace
                                .replaceAll(Pattern.quote(oldName), newName)
                ), "UTF-8");
    }
    
}
