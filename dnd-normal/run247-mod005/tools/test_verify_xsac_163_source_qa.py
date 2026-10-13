"""Offline negative-path checks of XSAC source trace gate, no network required."""
import unittest
import verify_xsac_163_source_qa as xsac

class XsacSourceTraceTests(unittest.TestCase):
    def test_pointer_supports_arrays_and_named_entries(self):
        data={"bookData":[{"data":[{"entries":[{"name":"hello","entries":["answer"]}]}]}]}
        self.assertEqual("hello",xsac.pointer(data,"/bookData/0/data/0/entries/0")["name"])

    def test_bad_pointer_is_a_hard_failure(self):
        with self.assertRaises(ValueError):
            xsac.pointer({"bookData":[]}, "/bookData/0/data/0")
        with self.assertRaises(ValueError):
            xsac.pointer({"a":1}, "/a")

    def test_nested_name_prevents_counting_category_as_qa(self):
        inp={"name":"Category","entries":[{"name":"Actual question?","id":"001","entries":["Yes."]}]}
        self.assertEqual([("/a/entries/0","spellcasting","sec_0_11","001",1)],
                         xsac.collect(inp,"/a","spellcasting","sec_0_11"))

    def test_qa_with_bulleted_answer_remains_a_single_qa(self):
        inp={"name":"Question?","id":"xyz","entries":[
             {"type":"list","items":[{"type":"item","name":"Heading","entry":"Rule"}]}]}
        self.assertEqual(1,len(xsac.collect(inp,"/a","combat","sec_0_4")))

    def test_pinned_blob_is_git_blob_not_file_sha(self):
        import hashlib
        data=b"test"
        expected=hashlib.sha1(b"blob 4\0test").hexdigest()
        self.assertEqual(expected,xsac.git_blob_sha(data))

if __name__ == "__main__":
    unittest.main()
