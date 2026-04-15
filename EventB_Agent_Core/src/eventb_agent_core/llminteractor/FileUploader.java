package eventb_agent_core.llminteractor;

import java.nio.file.Path;

import org.json.JSONObject;

import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMResponseParser;

/**
 * This class is responsible for uploading a file to the LLM platform.
 */
public class FileUploader extends AbstractLLMInteractor {

	public FileUploader(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	/**
	 * Upload file and return fileID.
	 * 
	 * @param inputPDFPath
	 * @throws ReachMaxAttemptException
	 */
	public String uploadFile(Path inputPDFPath) {
		JSONObject uploadFileResponse;
		try {
			uploadFileResponse = getLLMResponseUploadFile(inputPDFPath);
			String fileID = uploadFileResponse.getString("id");
			return fileID;
		} catch (ReachMaxAttemptException e) {
			System.out.println(e);
		}
		return "";
	}

}
