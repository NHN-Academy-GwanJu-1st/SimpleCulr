package com.nhnacadey.simplecurl;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class SimpleCurl {

    private static final String USER_AGENT = "User-Agent: curl/7.68.0";
    private static final String ACCEPT = "Accept: */*";

    private static int REDIRECT_COUNT = 0;

    private String url;
    private String method;
    private boolean verbos;
    private boolean header;
    private String headerKey;
    private String headerValue;
    private boolean data;
    private String dataValue;
    private boolean location;
    private boolean file;
    private String fileName;
    private String fileType;

    public SimpleCurl() {
        this.method = "GET";
        this.verbos = false;
        this.header = false;
        this.data = false;
        this.location = false;
    }

    public static void main(String[] args) throws IOException {

        if (!args[0].equals("scurl")) {
            throw new RuntimeException("Starting Command is <scurl>");
        }

        SimpleCurl simpleCurl = new SimpleCurl();

//        simpleCurl.optionCheck(args);

        start(simpleCurl, args);
    }

    private static void start(SimpleCurl simpleCurl, String[] args) throws IOException {

        simpleCurl.optionCheck(args);

        if (simpleCurl.file) {
            simpleCurl.method = "POST";
        }

        URL url = new URL(simpleCurl.url);

        Socket socket = new Socket();
        SocketAddress address = new InetSocketAddress(url.getHost(), 80);
        socket.connect(address);

        StringBuilder outBuilder = new StringBuilder();

        outBuilder.append(
                simpleCurl.method +" /"+url.getPath()+" HTTP/1.1"+"\n"+
                "Host: "+ url.getHost()+"\n"+
                USER_AGENT+"\n"+
                ACCEPT+"\n"
        );

        if (simpleCurl.header) {
            outBuilder.append(simpleCurl.headerKey + ": " + simpleCurl.headerValue + "\n");
        }
        if (simpleCurl.data) {
            outBuilder.append("Content-Length: " + simpleCurl.dataValue.length() + "\n");
        }
        if (simpleCurl.file) {
            outBuilder.append("Content-Type: " + simpleCurl.fileType + "\n");
        }

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(outBuilder);

        if (simpleCurl.data) {
            writer.println(simpleCurl.dataValue);
        }
        if (simpleCurl.file) {
//            File file = new File(simpleCurl.fileName);
            writer.println(simpleCurl.fileName);
        }

        writer.println();
        writer.flush();

        if (simpleCurl.isVerbos()) {
            System.out.println(outBuilder);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            redirectCheck(line, url, simpleCurl);

            if (!simpleCurl.isVerbos()) {
                if (!line.equals("{")) {
                    continue;
                } else {
                    simpleCurl.verbos = true;
                }
            }
            sb.append(line + "\n");
        }

        System.out.println(sb);
    }

    public boolean isVerbos() {
        return this.verbos;
    }

    public void optionCheck(String[] params) {

        for (int i = 0; i < params.length; i++) {
            if (params[i].contains("http")) {
                this.url = params[i];
            } else if (params[i].equals("-v")) {
                this.verbos = true;
            } else if (params[i].equals("-X")) {
                this.method = params[i + 1];
            } else if (params[i].equals("-H")) {
                StringTokenizer st = new StringTokenizer(params[i + 1], ": \"");
                this.header = true;
                this.headerKey = st.nextToken();
                this.headerValue = st.nextToken();
            } else if (params[i].equals("-d")) {
                this.data = true;
                this.dataValue = params[i + 1].replaceAll("\\\\", "");
            } else if (params[i].equals("-L")) {
                this.location = true;
            } else if (params[i].equals("-F")) {
                this.file = true;
                this.fileName = params[i + 1].split("@")[1];
                this.fileType = "multipart/form-data";
            }
        }
    }

    private static void redirectCheck(String line, URL url, SimpleCurl simpleCurl) throws IOException {
        if (line.contains("location") || line.contains("Location")) {
            String redirect = line.split(" ")[1];
             String[] redirectArgs = {"-v", "-L"};
            simpleCurl.url = "http://" + url.getHost() + redirect;
            REDIRECT_COUNT++;
            if (REDIRECT_COUNT > 5) {
                throw new RuntimeException("Too Many Redirect call");
            }
            start(simpleCurl, redirectArgs);
        }
    }


}
