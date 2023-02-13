package es.ucm.fdi.ac.walkers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import es.ucm.fdi.util.archive.ArchiveFormat;
import es.ucm.fdi.util.archive.ZipFormat;

class ReNode implements Comparable<ReNode> {
	public String name;
	public ParserRuleContext ctx;
	public int start;
	public int size;
	public String tokens;
	public List<ReNode> children = new ArrayList<>();
	public ReNode parent = null;
	public ReNode(ParserRuleContext ctx) {
		this.ctx = ctx;
		this.start = ((CommonToken)ctx.start).getStartIndex();
		this.size = ((CommonToken)ctx.stop).getStopIndex()-start+1;
		this.name = ctx.getClass().getSimpleName();
	}
	@Override
	public String toString() {
		return name + " @" + start + " +" + size + "\n";
	}
	@Override
	public int compareTo(ReNode n) {
		return Integer.compare(size, n.size);
	}		

	public String contents(String source) {
		return source.substring(start, start + size);
	}

	public String tokenizedContents(CommonTokenStream allTokens) {
		int startIdx = ((CommonToken)ctx.start).getTokenIndex();
		int stopIdx = ((CommonToken)ctx.stop).getTokenIndex();
		StringBuilder sb = new StringBuilder();
		for (int i=startIdx; i<stopIdx; i++) {
			//FIXME sb.append(Integer.toString(tokens.get(i).getType(), 32)); // base 32
			sb.append(" ");				
		}
		return sb.append("-1").toString();
	}

	public void fixStartsAndEnds(CommonTokenStream allTokens) {
		tokens = tokenizedContents(allTokens);
		if ( ! children.isEmpty()) {
			ReNode first = children.get(0);
			ReNode prev = first;
			for (int i=1; i<children.size(); i++) {
				ReNode n = children.get(i);
				int endOfPrev = prev.start + prev.size;
				if (endOfPrev < n.start) {
					// grow n so that it starts where the previous node ended
					n.size += n.start - endOfPrev;
					n.start = endOfPrev;
				}
			    prev = n;
			}
		}	
	}

	public void write(String source, PrintWriter out, Map<String, String> rs) {
		out.println("\n// " + tokens);				
		if (children.isEmpty()) {
			out.print(source.substring(start, start + size));				
		} else {
			ReNode first = children.get(0);
			ReNode last = children.get(children.size()-1);
			out.print(source.substring(start, first.start));
			Collections.sort(children);		
			for (ReNode n : children) {
				n.write(source, out, rs);
			}
			out.print(source.substring(last.start + last.size, start + size));
		}			
	}

	public static int compressedSize(ArchiveFormat compressor, String data) {
		try (InputStream ts = new ByteArrayInputStream(data.getBytes())) {
			return compressor.compressedSize(ts);
		} catch (Exception e) {
			SourceAligner.log.warn("Error testing compression on " + data, e);
			throw new IllegalArgumentException(e);
		}			
	}

	public float similarityWith(ReNode o) {
		ArchiveFormat compressor = new ZipFormat();
		int a = compressedSize(compressor, tokens);
		int b = compressedSize(compressor, o.tokens);
		int c = compressedSize(compressor, tokens + o.tokens);
		int m = Math.min(a, b);
		int M = a + b - m;
		return (float) (c - m) / (float) M;
	}
}