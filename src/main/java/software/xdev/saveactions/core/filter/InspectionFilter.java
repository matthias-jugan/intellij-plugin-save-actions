package software.xdev.saveactions.core.filter;

import java.util.List;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;


public interface InspectionFilter
{
	List<ProblemDescriptor> filter(final List<ProblemDescriptor> elements, final PsiFile file);
}
