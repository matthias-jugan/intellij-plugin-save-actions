package software.xdev.saveactions.core.filter;

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;


public final class AllFilter implements InspectionFilter
{
	private static final AllFilter INSTANCE = new AllFilter();
	
	public static AllFilter get()
	{
		return INSTANCE;
	}
	
	@Override
	public List<ProblemDescriptor> filter(final List<ProblemDescriptor> elements, final PsiFile file)
	{
		return new ArrayList<>(elements);
	}
}
