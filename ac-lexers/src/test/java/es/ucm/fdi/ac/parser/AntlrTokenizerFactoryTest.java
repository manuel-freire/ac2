package es.ucm.fdi.ac.parser;

import es.ucm.fdi.ac.Analysis;
import es.ucm.fdi.ac.Submission;
import es.ucm.fdi.ac.Tokenizer;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;

public class AntlrTokenizerFactoryTest extends TestCase {

	private static int lastId = 0;

	private static Submission mockSub(String... fileNames) {
		Submission s = new Submission("s" + lastId, "./s" + lastId, lastId);
		for (String name : fileNames) {
			s.addSource(new File(s.getOriginalPath() + "/" + name));
		}
		return s;
	}

	@Test
	public void testGetTokenizerFor() {
		AntlrTokenizerFactory factory = new AntlrTokenizerFactory();

		Submission[] subs = new Submission[] {
				mockSub("foo.c", "bar.h", "quux.whatever"),
				mockSub("VeryVerbose.java", "OverlyLong.java",
						"AnotherClass.java", "OrAnInterface.java"),
				mockSub("LotsOfCamelCase.java", "garbage.gar", "rubbish.rub",
						"tripe.tri") };

		Tokenizer t = factory.getTokenizerFor(subs);
		Tokenizer jt = AntlrTokenizerFactory.TokenizerEntry.forName("foo.java").tokenizer;
		assertEquals(jt, t);
	}
}