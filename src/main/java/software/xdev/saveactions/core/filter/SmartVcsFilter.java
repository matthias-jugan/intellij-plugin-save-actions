package software.xdev.saveactions.core.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.intellij.codeInsight.actions.VcsFacade;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.psi.util.PsiTreeUtil;


public class SmartVcsFilter implements InspectionFilter
{
	private static final SmartVcsFilter INSTANCE = new SmartVcsFilter();
	
	public static SmartVcsFilter get()
	{
		return INSTANCE;
	}
	
	private static final Class<? extends PsiElement>[] UPCAST_PSI_ELEMENTS = new Class[]{PsiMethod.class};
	
	public static PsiElement upcastPsiElement(final PsiElement element)
	{
		if(element == null)
		{
			return null;
		}
		final PsiElement upcastElement = PsiTreeUtil.getNonStrictParentOfType(element, UPCAST_PSI_ELEMENTS);
		return Objects.requireNonNullElse(upcastElement, element);
	}
	
	public static TextRange expandRange(final TextRange range, final PsiFile file)
	{
		final PsiElement startElement = upcastPsiElement(file.findElementAt(range.getStartOffset()));
		final PsiElement endElement = upcastPsiElement(file.findElementAt(range.getEndOffset()));
		TextRange expanded = range;
		if(startElement != null)
		{
			expanded = expanded.union(startElement.getTextRange());
		}
		if(endElement != null)
		{
			expanded = expanded.union(endElement.getTextRange());
		}
		return expanded;
	}
	
	@Override
	public List<ProblemDescriptor> filter(final List<ProblemDescriptor> elements, final PsiFile file)
	{
		final ChangedRangesInfo changedInfo = VcsFacade.getInstance().getChangedRangesInfo(file);
		if(changedInfo == null)
		{
			return new ArrayList<>(elements);
		}
		final List<TextRange> expandedChangedRanges =
			changedInfo.allChangedRanges.stream().map(range -> expandRange(range, file)).toList();
		return elements.stream().filter(element -> {
			final TextRange fixedRange = element.getPsiElement().getTextRange();
			return expandedChangedRanges.stream().anyMatch(changed -> changed.intersects(fixedRange));
		}).toList();
	}
}
