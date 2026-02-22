package beastfx.app.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import beast.base.core.Log;





public class HTMLPrintStream extends PrintStream {
	public static Log.Level currentLevel = null;
	
	Log.Level level = Log.Level.info;
	
	public HTMLPrintStream(OutputStream out) {
		super(out);
	}
	
	public HTMLPrintStream(OutputStream out, Log.Level level) {
		super(out);
		this.level = level;
	}
	
	@Override
    public void write(byte buf[], int off, int len) {
        if (buf == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > buf.length) || (len < 0) ||
                   ((off + len) > buf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        for (int i = 0 ; i < len ; i++) {
            write(buf[off + i]);
        }
    }
	
	@Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }
	
	@Override
	public void write(int b) {
        try {
            synchronized (this) {
            	if (this.level != currentLevel){
            		currentLevel = this.level;
            		String div = "</span><span class='logLevel" + currentLevel+ "'>";
            		out.write(div.getBytes());
            	}
				switch (b) {
				case '\n':
                	out.write('<');
                	out.write('b');
                	out.write('r');
                	out.write('>');
      				out.write(b);
                    out.flush();
                    break;
				case '<':
                	out.write('&');
                	out.write('l');
                	out.write('t');
                	out.write(';');
                    break;
				case '>':
                	out.write('&');
                	out.write('g');
                	out.write('t');
                	out.write(';');
                    break;
				case '&':
                	out.write('&');
                	out.write('a');
                	out.write('m');
                	out.write('p');
                	out.write(';');
                    break;
				case '"':
                	out.write('&');
                	out.write('q');
                	out.write('u');
                	out.write('o');
                	out.write('t');
                	out.write(';');
                    break;
				case '\'':
                	out.write('&');
                	out.write('a');
                	out.write('p');
                	out.write('o');
                	out.write('s');
                	out.write(';');
                    break;
				case ' ':
                	out.write(' ');
                	out.write('&');
                	out.write('n');
                	out.write('b');
                	out.write('s');
                	out.write('p');
                	out.write(';');
                    break;
				case '\t':
					for (int i = 0; i < 4; i++) {
	                	out.write('&');
	                	out.write('n');
	                	out.write('b');
	                	out.write('s');
	                	out.write('p');
	                	out.write(';');
					}
                    break;
                 default:
      				out.write(b);

	            }
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
