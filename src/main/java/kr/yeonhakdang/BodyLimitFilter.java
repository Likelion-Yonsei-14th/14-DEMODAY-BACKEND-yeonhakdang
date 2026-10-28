package kr.yeonhakdang;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;

/** Bounds JSON bodies too, including chunked requests with no Content-Length. */
@Component
public class BodyLimitFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(!req.getRequestURI().startsWith("/api/")||!java.util.Set.of("POST","PUT","PATCH").contains(req.getMethod())) {chain.doFilter(req,res);return;}
        // Read at most 64KiB + 1 byte, before any JSON parsing or signature validation.
        byte[] bytes=req.getInputStream().readNBytes(65537);
        if(bytes.length>65536) {SecurityConfig.json(res,413,"REQUEST_TOO_LARGE");return;}
        chain.doFilter(new HttpServletRequestWrapper(req){
            @Override public ServletInputStream getInputStream() {
                var input=new ByteArrayInputStream(bytes);
                return new ServletInputStream(){
                    public int read(){return input.read();}
                    public int read(byte[] b,int off,int len){return input.read(b,off,len);}
                    public boolean isFinished(){return input.available()==0;}
                    public boolean isReady(){return true;}
                    public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}
                };
            }
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
        },res);
    }
}
