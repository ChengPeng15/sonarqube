/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualitygate;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();

  private QualityGateFinder underTest = new QualityGateFinder(db.getDbClient());

  @Test
  public void return_default_quality_gate_for_project() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto dbQualityGate = db.qualityGates().createDefaultQualityGate("Sonar way");

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, project.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getQualityGate().getId()).isEqualTo(dbQualityGate.getId());
    assertThat(result.get().isDefault()).isTrue();
  }

  @Test
  public void return_project_quality_gate_over_default() {
    ComponentDto project = db.components().insertPrivateProject();
    db.qualityGates().createDefaultQualityGate("Sonar way");
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate("My team QG");
    db.qualityGates().associateProjectToQualityGate(project, dbQualityGate);

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, project.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getQualityGate().getId()).isEqualTo(dbQualityGate.getId());
    assertThat(result.get().isDefault()).isFalse();
  }

  @Test
  public void return_nothing_when_no_default_qgate_and_no_qgate_defined_for_project() {
    ComponentDto project = db.components().insertPrivateProject();

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, project.getId());

    assertThat(result).isNotPresent();
  }

  @Test
  public void fail_when_default_qgate_defined_in_properties_does_not_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto dbQualityGate = db.qualityGates().createDefaultQualityGate("Sonar way");
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    assertThat(underTest.getQualityGate(dbSession, project.getId())).isEmpty();
  }

  @Test
  public void fail_when_project_qgate_defined_in_properties_does_not_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate("My team QG");
    db.qualityGates().associateProjectToQualityGate(project, dbQualityGate);
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    expectedException.expect(NotFoundException.class);
    underTest.getQualityGate(dbSession, project.getId());
  }

  @Test
  public void get_by_name_or_id() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThat(underTest.getByNameOrId(db.getSession(), qualityGate.getName(), null)).isNotNull();
    assertThat(underTest.getByNameOrId(db.getSession(), null, qualityGate.getId())).isNotNull();
  }

  @Test
  public void fail_get_by_name_or_id_when_name_matches_nothing() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No quality gate has been found for name UNKNOWN");

    underTest.getByNameOrId(db.getSession(), "UNKNOWN", null);
  }

  @Test
  public void fail_get_by_name_or_id_when_id_matches_nothing() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No quality gate has been found for id 123");

    underTest.getByNameOrId(db.getSession(), null, 123L);
  }

  @Test
  public void fail_get_by_name_or_id_when_parameters_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("No parameter has been set to identify a quality gate");

    underTest.getByNameOrId(db.getSession(), null, null);
  }
}
